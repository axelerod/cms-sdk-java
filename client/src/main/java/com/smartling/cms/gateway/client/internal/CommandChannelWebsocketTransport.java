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

import java.net.URI;
import java.util.concurrent.Future;

import javax.websocket.ContainerProvider;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 * Websocket transport for command channel.
 *
 * @author p.ivashkov
 */
public class CommandChannelWebsocketTransport implements CommandChannelTransport
{
    private WebSocketContainer container;
    private Session session;

    public CommandChannelWebsocketTransport()
    {
        container = ContainerProvider.getWebSocketContainer();
    }

    @Override
    public void connectToServer(Object annotatedEndpoint, URI path) throws Exception
    {
        session = container.connectToServer(annotatedEndpoint, path);
    }

    @Override
    public Future<Void> send(String text)
    {
        Async remote = session.getAsyncRemote();
        return remote.sendText(text);
    }
}