/*
 * Copyright 2014 Smartling, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this work except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smartling.cms.gateway.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Future;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.apache.commons.lang3.Validate;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import com.smartling.cms.gateway.client.api.model.ResponseStatus;
import com.smartling.cms.gateway.client.command.BaseCommand;
import com.smartling.cms.gateway.client.command.CommandChannelHandler;
import com.smartling.cms.gateway.client.command.ErrorResponse;
import com.smartling.cms.gateway.client.command.GetHtmlCommand;
import com.smartling.cms.gateway.client.command.GetResourceCommand;
import com.smartling.cms.gateway.client.internal.CommandChannelTransport;
import com.smartling.cms.gateway.client.internal.CommandChannelWebsocketTransport;
import com.smartling.cms.gateway.client.internal.CommandParser;
import com.smartling.cms.gateway.client.internal.ResponseStatusFuture;
import com.smartling.cms.gateway.client.upload.FileUpload;

/**
 * Client to CMS Gateway service public API.
 *
 * @author p.ivashkov
 *
 * {@code
 *  CmsGatewayClient client = CmsGatewayClientBuilder.create()
 *      .setApiKey(YOUR_API_KEY);
 *      .setProjectId(YOUR_PROJECT_ID);
 *      .build();
*   client.connect(commandHandler);
 * }
 */
public class CmsGatewayClient implements Closeable
{
    public static final String DEFAULT_COMMAND_CHANNEL_ENDPOINT = "ws://localhost/cmd/websocket";
    public static final String DEFAULT_UPLOAD_CHANNEL_ENDPOINT = "http://localhost/upload";

    private final URI commandChannelUri;
    private final URI uploadChannelUri;
    private final CommandChannelTransport commandChannel;
    private final CloseableHttpAsyncClient uploadChannel;
    private final CommandParser commandParser;
    private CommandChannelHandler handler;

    public CmsGatewayClient(URI commandChannelUri, URI uploadChannelUri, CommandChannelTransport commandChannelTransport,
            CloseableHttpAsyncClient httpAsyncClient, CommandParser commandParser)
    {
        this.commandChannelUri = Validate.notNull(commandChannelUri);
        this.uploadChannelUri = Validate.notNull(uploadChannelUri);
        commandChannel = Validate.notNull(commandChannelTransport);
        uploadChannel = Validate.notNull(httpAsyncClient);
        this.commandParser = Validate.notNull(commandParser);
    }

    public void connect(CommandChannelHandler commandChannelHandler) throws CmsGatewayClientException
    {
        handler = Validate.notNull(commandChannelHandler);

        try
        {
            uploadChannel.start();
            commandChannel.connectToServer(new CommandChannelTransportEndpoint(), commandChannelUri);
        }
        catch (Exception e)
        {
            throw new CmsGatewayClientException(e);
        }
    }

    private URI getUploadChannelUri(String requestId) throws CmsGatewayClientException
    {
        try
        {
            return new URIBuilder(uploadChannelUri)
                .addParameter("rid", requestId)
                .build();
        }
        catch (URISyntaxException e)
        {
            throw new CmsGatewayClientException(e);
        }
    }

    public void close() throws IOException
    {
        uploadChannel.close();
        commandChannel.close();
    }

    public void send(ErrorResponse error)
    {
        commandChannel.send(error.toJSONString());
    }

    public Future<ResponseStatus<Void>> send(FileUpload response) throws CmsGatewayClientException, IOException
    {
        String requestId = response.getRequest().getId();
        URI uploadUri = getUploadChannelUri(requestId);
        HttpPost post = new HttpPost(uploadUri);
        post.setEntity(response.getHttpEntity());
        return new ResponseStatusFuture(uploadChannel.execute(post, null));
    }

    @ClientEndpoint
    public class CommandChannelTransportEndpoint
    {
        @OnOpen
        public void onOpen(Session session, EndpointConfig config)
        {
        }

        @OnClose
        public void onClose(Session session, CloseReason reason)
        {
            if (!reason.getCloseCode().equals(CloseCodes.NORMAL_CLOSURE))
            {
                try
                {
                    reopenCommandChannel();
                }
                catch (CmsGatewayClientException e)
                {
                    handler.onError(e);
                    handler.onDisconnect();
                }
            }
            else
            {
                handler.onDisconnect();
            }
        }

        @OnError
        public void onError(Session session, Throwable e)
        {
            handler.onError(e);
        }

        @OnMessage
        public void onMessage(String message, Session session)
        {
            try
            {
                BaseCommand request = commandParser.parse(message);
                onCommand(session, request);
            }
            catch (Throwable e)
            {
                handler.onError(e);
            }
        }

        private void reopenCommandChannel() throws CmsGatewayClientException
        {
            try
            {
                commandChannel.connectToServer(this, commandChannelUri);
            }
            catch (Throwable e)
            {
                throw new CmsGatewayClientException(e);
            }
        }

        private void onCommand(Session session, BaseCommand request)
        {
            switch(request.getType())
            {
            case AUTHENTICATION_ERROR:

                try
                {
                    session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "authentication error"));
                }
                catch (IOException ignored)
                {
                }

                handler.onError(new CmsGatewayClientAuthenticationException());
                break;
            case AUTHENTICATION_SUCCESS:
                handler.onConnect();
                break;
            case GET_HTML:
                handler.onGetHtmlCommand((GetHtmlCommand) request);
                break;
            case GET_RESOURCE:
                handler.onGetResourceCommand((GetResourceCommand) request);
                break;
            }
        }
    }

}
