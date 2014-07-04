package com.smartling.cms.gateway.client.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

    @InjectMocks
    private CommandChannelWebsocketTransport transport;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        when(container.connectToServer(anyObject(), any(URI.class))).thenReturn(session);
        when(session.getAsyncRemote()).thenReturn(remote);
        when(session.isOpen()).thenReturn(true);

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
        verify(pingTimer).schedule(captor.capture(), anyInt(), anyInt());

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