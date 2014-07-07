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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CommandChannelWebsocketTransport.class)
public class CommandChannelWebsocketTransportTest
{
    private static final long STUB_HEARTBEAT = 3;

    @Mock
    private WebSocketContainer container;

    @Mock
    private Session session;

    @Mock
    private RemoteEndpoint.Async remote;

    @Mock
    private Timer pingTimer;

    private CommandChannelWebsocketTransport transport;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        whenNew(Timer.class).withNoArguments().thenReturn(pingTimer);


        when(container.connectToServer(anyObject(), any(URI.class))).thenReturn(session);
        when(session.getAsyncRemote()).thenReturn(remote);
        when(session.isOpen()).thenReturn(true);

        transport = new CommandChannelWebsocketTransport(container);
        transport.setHeartbeatInterval(STUB_HEARTBEAT);
    }

    @Test
    public void startsPingTimerOnConnect() throws Exception
    {
        transport.connectToServer(null, null);

        verify(pingTimer).schedule(any(TimerTask.class), eq(STUB_HEARTBEAT), eq(STUB_HEARTBEAT));
    }

    @Test
    public void doesNotScheduleTimerForZeroHeartbeatInterval() throws Exception
    {
        transport.setHeartbeatInterval(0);

        transport.connectToServer(null, null);

        verifyZeroInteractions(pingTimer);
    }

    @Test
    public void pingsServerOnTimerTick() throws Exception
    {
        TimerTask timerTask = getPingTimerTask();

        timerTask.run();

        verify(remote).sendPong(null);
    }

    private TimerTask getPingTimerTask() throws Exception
    {
        transport.connectToServer(null, null);

        ArgumentCaptor<TimerTask> captor = ArgumentCaptor.forClass(TimerTask.class);
        verify(pingTimer).schedule(captor.capture(), anyLong(), anyLong());

        return captor.getValue();
    }

    @Test
    public void doesNotPingWhenSessionIsClosed() throws Exception
    {
        when(session.isOpen()).thenReturn(false);
        TimerTask timerTask = getPingTimerTask();

        timerTask.run();

        verifyZeroInteractions(remote);
    }
}