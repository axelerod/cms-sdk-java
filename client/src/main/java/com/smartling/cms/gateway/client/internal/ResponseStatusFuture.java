package com.smartling.cms.gateway.client.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;

import com.google.gson.Gson;
import com.smartling.cms.gateway.client.CmsGatewayClientException;
import com.smartling.cms.gateway.client.api.model.ResponseStatus;
import com.smartling.cms.gateway.client.api.model.ResponseWrapper;

/**
 * Extracts ResponseStatus from HttpResponse.
 */
public class ResponseStatusFuture implements Future<ResponseStatus<Void>>
{
    private final Future<HttpResponse> future;

    public ResponseStatusFuture(Future<HttpResponse> future)
    {
        this.future = future;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled()
    {
        return future.isCancelled();
    }

    @Override
    public boolean isDone()
    {
        return future.isDone();
    }

    @Override
    public ResponseStatus<Void> get() throws InterruptedException, ExecutionException
    {
        HttpResponse httpResponse = future.get();
        try
        {
            return responseStatus(httpResponse);
        }
        catch (IOException e)
        {
            throw new ExecutionException(e);
        }
        catch (CmsGatewayClientException e)
        {
            throw new ExecutionException(e);
        }
    }

    @Override
    public ResponseStatus<Void> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
    {
        HttpResponse httpResponse = future.get(timeout, unit);
        try
        {
            return responseStatus(httpResponse);
        }
        catch (IOException e)
        {
            throw new ExecutionException(e);
        }
        catch (CmsGatewayClientException e)
        {
            throw new ExecutionException(e);
        }
    }

    private ResponseStatus<Void> responseStatus(HttpResponse httpResponse) throws IOException, CmsGatewayClientException
    {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        InputStreamReader streamReader = new InputStreamReader(httpResponse.getEntity().getContent());
        Gson gson = new Gson();
        ResponseWrapper wrapper = gson.fromJson(streamReader, ResponseWrapper.class);

        @SuppressWarnings("unchecked")
        ResponseStatus<Void> responseStatus = wrapper.getResponse();

        if (statusCode != 200)
        {
            String message = String.format("Upload failed (%d): %s %s", statusCode, responseStatus.getCode(), StringUtils.join(responseStatus.getMessages(), ' '));
            throw new CmsGatewayClientException(message);
        }

        return responseStatus;
    }
}
