package com.smartling.cms.gateway.client.command;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Thread.class)
public class ReconnectStrategyTest
{
    private ReconnectStrategy strategy = new ReconnectStrategy();

    @Before
    public void setUp() throws Exception
    {
        mockStatic(Thread.class);
    }

    @Test
    public void incrementsDelayIntervalOnConsecutiveCalls() throws Exception
    {
        strategy.delay();
        assertThat(strategy.getDelayInterval(), is(1000L));

        strategy.delay();
        assertThat(strategy.getDelayInterval(), is(3000L));
    }

    @Test
    public void keepsRetryWhileErrorsAreIOException()
    {
        assertTrue(strategy.shouldRetry());

        strategy.observeError(mock(IOException.class));
        assertTrue(strategy.shouldRetry());

        strategy.observeError(mock(Exception.class));
        assertFalse(strategy.shouldRetry());
    }
}
