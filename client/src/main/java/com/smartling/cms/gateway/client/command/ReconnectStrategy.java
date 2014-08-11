package com.smartling.cms.gateway.client.command;

import java.io.IOException;

/**
 * Implements client reconnect strategy on connection errors.
 */
public class ReconnectStrategy
{
    private Exception lastError;
    private long delayInterval;
    private boolean retry = true;

    public boolean shouldRetry()
    {
        return retry;
    }

    public void observeError(Exception e)
    {
        lastError = e;
        retry = retry && (e instanceof IOException);
    }

    public Exception getLastError()
    {
        return lastError;
    }

    public long getDelayInterval()
    {
        return delayInterval;
    }

    public void delay() throws InterruptedException
    {
        if (delayInterval > 0)
        {
            Thread.sleep(delayInterval);
        }

        delayInterval = (delayInterval + 500) * 2;
    }

    public ReconnectStrategy reset()
    {
        lastError = null;
        delayInterval = 0;
        retry = true;

        return this;
    }
}
