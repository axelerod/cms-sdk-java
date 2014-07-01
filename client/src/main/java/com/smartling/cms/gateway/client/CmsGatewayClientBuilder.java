package com.smartling.cms.gateway.client;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import com.smartling.cms.gateway.client.command.CommandChannelHandler;
import com.smartling.cms.gateway.client.internal.CommandChannelTransport;
import com.smartling.cms.gateway.client.internal.CommandChannelWebsocketTransport;
import com.smartling.cms.gateway.client.internal.CommandParser;

/**
 * Builder for {@link CmsGatewayClient} instances.
 */
public class CmsGatewayClientBuilder
{
    private String commandChannelEndpoint = CmsGatewayClient.DEFAULT_COMMAND_CHANNEL_ENDPOINT;
    private String uploadChannelEndpoint = CmsGatewayClient.DEFAULT_UPLOAD_CHANNEL_ENDPOINT;
    private String apiKey;
    private String projectId;
    private CommandChannelTransport commandChannelTransport = new CommandChannelWebsocketTransport();
    private CloseableHttpAsyncClient uploadChannelTransport;
    private CommandParser commandParser = new CommandParser();

    protected CmsGatewayClientBuilder()
    {
    }

    public static CmsGatewayClientBuilder create()
    {
        return new CmsGatewayClientBuilder();
    }

    public final CmsGatewayClientBuilder setApiKey(final String apiKey)
    {
        this.apiKey = apiKey;
        return this;
    }

    public final CmsGatewayClientBuilder setProjectId(final String projectId)
    {
        this.projectId = projectId;
        return this;
    }

    public final CmsGatewayClientBuilder setCommandChannelEndpoint(final String commandChannelEndpoint)
    {
        this.commandChannelEndpoint = commandChannelEndpoint;
        return this;
    }

    public final CmsGatewayClientBuilder setUploadChannelEndpoint(final String uploadChannelEndpoint)
    {
        this.uploadChannelEndpoint = uploadChannelEndpoint;
        return this;
    }

    public final CmsGatewayClientBuilder setCommandChannelTransport(final CommandChannelTransport commandChannelTransport)
    {
        this.commandChannelTransport = commandChannelTransport;
        return this;
    }

    public final CmsGatewayClientBuilder setUploadChannelTransport(final CloseableHttpAsyncClient uploadChannelTransport)
    {
        this.uploadChannelTransport = uploadChannelTransport;
        return this;
    }

    public final CmsGatewayClientBuilder setCommandParser(final CommandParser commandParser)
    {
        this.commandParser = commandParser;
        return this;
    }

    public CmsGatewayClient build() throws CmsGatewayClientException
    {
        CloseableHttpAsyncClient uploadChannel = this.uploadChannelTransport;
        if (uploadChannel == null)
        {
            uploadChannel = HttpAsyncClients.createDefault();
        }

        return new CmsGatewayClient(
                getCommandChannelUri(),
                getUploadChannelUri(),
                commandChannelTransport,
                uploadChannel,
                commandParser
        );
    }

    private URI getCommandChannelUri() throws CmsGatewayClientException
    {
        try
        {
            return new URIBuilder(URI.create(commandChannelEndpoint))
                .addParameter("key", apiKey)
                .addParameter("projectId", projectId)
                .build();
        }
        catch (URISyntaxException e)
        {
            throw new CmsGatewayClientException(e);
        }
    }

    private URI getUploadChannelUri() throws CmsGatewayClientException
    {
        try
        {
            return new URIBuilder(uploadChannelEndpoint)
                    .addParameter("key", apiKey)
                    .addParameter("projectId", projectId)
                    .build();
        }
        catch (URISyntaxException e)
        {
            throw new CmsGatewayClientException(e);
        }
    }
}
