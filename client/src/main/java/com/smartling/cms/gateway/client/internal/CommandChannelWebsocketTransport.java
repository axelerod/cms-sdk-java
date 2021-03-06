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
package com.smartling.cms.gateway.client.internal;

import java.io.IOException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;

import javax.websocket.DeploymentException;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import com.smartling.cms.gateway.client.CmsGatewayClientException;

/**
 * Websocket transport for command channel.
 *
 */
public class CommandChannelWebsocketTransport implements CommandChannelTransport
{
    public static final long DEFAULT_HEARTBEAT_INTERVAL = 40000;

    private static final Logger logger = Logger.getLogger(CommandChannelWebsocketTransport.class);

    private final WebSocketContainer container;
    private long heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;

    public CommandChannelWebsocketTransport(WebSocketContainer container)
    {
        this.container = Validate.notNull(container);
    }

    @Override
    public CommandChannelSession connectToServer(Object annotatedEndpoint, URI path) throws IOException, CmsGatewayClientException
    {
        logger.debug(String.format("Connecting to command channel at %s", path));
        try
        {
            Session session = container.connectToServer(annotatedEndpoint, path);
            return new WebsocketSession(session, heartbeatInterval);
        }
        catch (DeploymentException e)
        {
            throw new CmsGatewayClientException(e);
        }
    }

    @Override
    public void setHeartbeatInterval(long heartbeatInterval)
    {
        this.heartbeatInterval = heartbeatInterval;
    }


    private static class WebsocketSession implements CommandChannelSession
    {
        private final Session session;
        private final Timer pingTimer = new Timer();

        private WebsocketSession(Session session, long heartbeatInterval)
        {
            this.session = Validate.notNull(session);

            if (heartbeatInterval > 0)
            {
                pingTimer.schedule(new PingTimerTask(), heartbeatInterval, heartbeatInterval);
            }
        }

        @Override
        public Future<Void> send(String text)
        {
            Async remote = session.getAsyncRemote();
            return remote.sendText(text);
        }

        @Override
        public void close() throws IOException
        {
            session.close();
            pingTimer.cancel();
        }

        private class PingTimerTask extends TimerTask
        {
            @Override
            public void run()
            {
                try
                {
                    if (session.isOpen())
                    {
                        // A Pong frame MAY be sent unsolicited. This serves as a unidirectional heartbeat.
                        session.getAsyncRemote().sendPong(null);
                    }
                }
                catch (IOException e)
                {
                    logger.error("Failed to pong server", e);
                }
            }
        }
    }

}
