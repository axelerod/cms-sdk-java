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
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.nio.client.HttpAsyncClient;

import com.smartling.cms.gateway.client.internal.CommandChannelTransport;
import com.smartling.cms.gateway.client.internal.CommandChannelWebsocketTransport;
import com.smartling.cms.gateway.client.internal.CommandParser;

/**
 * Client to CMS Gateway service public API.
 *
 * @author p.ivashkov
 *
 * {@code
 *  CmsGatewayClient client = new CmsGatewayClient();
 *  client.setApiKey(YOUR_API_KEY);
 *  client.setProjectId(YOUR_PROJECT_ID);
 *  client.openCommandChannel(commandsHandler);
 * }
 */
public class CmsGatewayClient
{
    public static final String DEFAULT_COMMAND_CHANNEL_ENDPOINT = "ws://localhost/cmd/websocket";
    public static final String DEFAULT_UPLOAD_CHANNEL_ENDPOINT = "http://localhost/upload";

    private String commandChannelEndpoint = DEFAULT_COMMAND_CHANNEL_ENDPOINT;
    private String uploadChannelEndpoint = DEFAULT_UPLOAD_CHANNEL_ENDPOINT;
    private String apiKey;
    private String projectId;
    private CommandChannelTransport commandChannel;
    private CommandChannelTransportEndpoint endpoint;
    private HttpAsyncClient uploadChannel;

    public CmsGatewayClient()
    {
    }

    public String getCommandChannelEndpoint()
    {
        return commandChannelEndpoint;
    }
    public void setCommandChannelEndpoint(String value)
    {
        commandChannelEndpoint = Validate.notNull(value);
    }

    public String getApiKey()
    {
        return apiKey;
    }
    public void setApiKey(String value)
    {
        apiKey = value;
    }

    public String getProjectId()
    {
        return projectId;
    }
    public void setProjectId(String value)
    {
        projectId = value;
    }

    public URI getCommandChannelUri() throws CmsGatewayClientException
    {
        Validate.notNull(getApiKey(), "Missing apiKey");
        Validate.notNull(getProjectId(), "Missing projectId");

        try
        {
            return new URIBuilder(getCommandChannelEndpoint())
                .addParameter("key", getApiKey())
                .addParameter("projectId", getProjectId())
                .build();
        }
        catch (URISyntaxException e)
        {
            throw new CmsGatewayClientException(e);
        }
    }

    public void setUploadChannelEndpoint(String value)
    {
        Validate.notNull(value);
        uploadChannelEndpoint = value;
    }
    public String getUploadChannelEndpoint()
    {
        return uploadChannelEndpoint;
    }

    public URI getUploadChannelUri(String requestId) throws CmsGatewayClientException
    {
        Validate.notNull(getApiKey(), "Missing apiKey");

        try
        {
            return new URIBuilder(getUploadChannelEndpoint())
                .addParameter("key", getApiKey())
                .addParameter("rid", requestId)
                .build();
        }
        catch (URISyntaxException e)
        {
            throw new CmsGatewayClientException(e);
        }
    }

    public void setHandler(CommandChannelHandler handler)
    {
        Validate.notNull(handler);
        endpoint = new CommandChannelTransportEndpoint(handler);
    }

    public void openCommandChannel(CommandChannelHandler handler) throws CmsGatewayClientException
    {
        setHandler(handler);
        reopenCommandChannel();
    }

    public void send(ErrorResponse error)
    {
        commandChannel.send(error.toJSONString());
    }

    public Future<HttpResponse> send(FileUpload response) throws CmsGatewayClientException
    {
        String requestId = response.getRequest().getId();
        URI uploadUri = getUploadChannelUri(requestId);
        HttpPost post = new HttpPost(uploadUri);
        post.setEntity(response.getHttpEntity());
        return uploadChannel.execute(post, null);
    }

    @ClientEndpoint
    public class CommandChannelTransportEndpoint
    {
        private CommandChannelHandler handler;
        private CommandParser commandParser;

        public CommandChannelTransportEndpoint(CommandChannelHandler handler)
        {
            this.handler = handler;
            commandParser = new CommandParser();
        }

        @OnOpen
        public void onOpen(Session session, EndpointConfig config)
        {
            handler.onConnect();
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
                CommandBase request = getCommandParser().parse(message);
                onCommand(request);
            }
            catch (Throwable e)
            {
                handler.onError(e);
            }
        }

        CommandParser getCommandParser()
        {
            return commandParser;
        }
        void setCommandParser(CommandParser commandParser)
        {
            this.commandParser = commandParser;
        }

        private void onCommand(CommandBase request)
        {
            switch(request.getType())
            {
            case AUTHENTICATION_ERROR:
                handler.onError(new CmsGatewayClientAuthenticationException());
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

    CommandChannelTransportEndpoint getTransportEndpoint()
    {
        return endpoint;
    }

    CommandChannelTransport getCommandChannelTransport()
    {
        return commandChannel;
    }
    void setCommandChannelTransport(CommandChannelTransport value)
    {
        Validate.notNull(value);
        commandChannel = value;
    }

    void setUploadChannel(HttpAsyncClient value)
    {
        Validate.notNull(value);
        uploadChannel = value;
    }

    private void reopenCommandChannel() throws CmsGatewayClientException
    {
        if (commandChannel == null)
        {
            commandChannel = new CommandChannelWebsocketTransport();
        }

        try
        {
            commandChannel.connectToServer(getTransportEndpoint(), getCommandChannelUri());
        }
        catch (Throwable e)
        {
            throw new CmsGatewayClientException(e);
        }
    }
}
