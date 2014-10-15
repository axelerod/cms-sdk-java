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
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;

import org.apache.commons.lang3.Validate;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.log4j.Logger;

import com.smartling.cms.gateway.client.api.model.ResponseStatus;
import com.smartling.cms.gateway.client.command.BaseCommand;
import com.smartling.cms.gateway.client.command.CommandChannelHandler;
import com.smartling.cms.gateway.client.command.DisconnectCommand;
import com.smartling.cms.gateway.client.command.ErrorResponse;
import com.smartling.cms.gateway.client.command.GetHtmlCommand;
import com.smartling.cms.gateway.client.command.GetResourceCommand;
import com.smartling.cms.gateway.client.command.ReconnectStrategy;
import com.smartling.cms.gateway.client.internal.CommandChannelSession;
import com.smartling.cms.gateway.client.internal.CommandChannelTransport;
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

    private static final Logger logger = Logger.getLogger(CmsGatewayClient.class);

    private final URI commandChannelUri;
    private final URI uploadChannelUri;
    private final CommandChannelTransport commandChannelTransport;
    private final CloseableHttpAsyncClient uploadChannel;
    private final CommandParser commandParser;
    private final ReconnectStrategy reconnectStrategy;
    private ConnectionManager connectionManager;
    private CommandChannelSession commandChannel;
    private CommandChannelHandler handler;
    private boolean closed;

    public CmsGatewayClient(URI commandChannelUri, URI uploadChannelUri, CommandChannelTransport commandChannelTransport,
            CloseableHttpAsyncClient httpAsyncClient, CommandParser commandParser, ReconnectStrategy reconnectStrategy)
    {
        this.commandChannelUri = Validate.notNull(commandChannelUri);
        this.uploadChannelUri = Validate.notNull(uploadChannelUri);
        this.commandChannelTransport = Validate.notNull(commandChannelTransport);
        uploadChannel = Validate.notNull(httpAsyncClient);
        this.commandParser = Validate.notNull(commandParser);
        this.reconnectStrategy = Validate.notNull(reconnectStrategy);
    }

    public void connect(CommandChannelHandler commandChannelHandler) throws CmsGatewayClientException
    {
        failIfClosed();

        handler = Validate.notNull(commandChannelHandler);
        connectionManager = new ConnectionManager(reconnectStrategy.reset(), new CommandChannelTransportEndpoint());
        commandChannel = connectionManager.reconnect();
    }

    private void failIfClosed()
    {
        if (closed)
            throw new IllegalStateException("Client is closed");
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
        closed = true;
        uploadChannel.close();
        commandChannel.close();
    }

    public void send(ErrorResponse error)
    {
        failIfClosed();
        commandChannel.send(error.toJSONString());
    }

    public Future<ResponseStatus<Void>> send(FileUpload response) throws CmsGatewayClientException, IOException
    {
        failIfClosed();

        String requestId = response.getRequest().getId();
        URI uploadUri = getUploadChannelUri(requestId);
        HttpPost post = new HttpPost(uploadUri);
        post.setEntity(response.getHttpEntity());
        return new ResponseStatusFuture(uploadChannel.execute(post, null));
    }


    private class ConnectionManager
    {
        private final ReconnectStrategy reconnectStrategy;
        private final CommandChannelTransportEndpoint commandChannelTransportEndpoint;

        public ConnectionManager(ReconnectStrategy reconnectStrategy, CommandChannelTransportEndpoint commandChannelTransportEndpoint)
        {
            this.reconnectStrategy = reconnectStrategy;
            this.commandChannelTransportEndpoint = commandChannelTransportEndpoint;
        }

        public CommandChannelSession reconnect() throws CmsGatewayClientException
        {
            uploadChannel.start();
            do
            {
                try
                {
                    reconnectStrategy.delay();
                    CommandChannelSession session = commandChannelTransport.connectToServer(commandChannelTransportEndpoint, commandChannelUri);
                    reconnectStrategy.reset();
                    return session;
                }
                catch (IOException e)
                {
                    reconnectStrategy.observeError(e);
                    logger.warn("Failed on reconnect", e);
                }
                catch (InterruptedException e)
                {
                    reconnectStrategy.observeError(e);
                    logger.debug("Interrupted reconnect", e);
                }
            }
            while (reconnectStrategy.shouldRetry());

            throw new CmsGatewayClientException(reconnectStrategy.getLastError());
        }
    }


    @ClientEndpoint
    public class CommandChannelTransportEndpoint
    {
        @OnClose
        public void onClose(Session session, CloseReason reason)
        {
            if (reason.getCloseCode().equals(CloseCodes.NORMAL_CLOSURE))
            {
                handler.onDisconnect();
                return;
            }

            try
            {
                connectionManager.reconnect();
            }
            catch (Exception e)
            {
                handler.onError(e);
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

        private void onCommand(Session session, BaseCommand request)
        {
            switch(request.getType())
            {
            case DISCONNECT:
                closeSession(session, new CloseReason(CloseCodes.NORMAL_CLOSURE, "disconnect command"));
                String disconnectReason = String.format("Disconnect command: %s", ((DisconnectCommand)request).getReasonMessage());
                handler.onError(new CmsGatewayClientException(disconnectReason));
                break;
            case AUTHENTICATION_ERROR:
                closeSession(session, new CloseReason(CloseCodes.NORMAL_CLOSURE, "authentication error"));
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

        private void closeSession(Session session, CloseReason closeReason)
        {
            try
            {
                session.close(closeReason);
            }
            catch (IOException ignored)
            {
            }
        }
    }

}
