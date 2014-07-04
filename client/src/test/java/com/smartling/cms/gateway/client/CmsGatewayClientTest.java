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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.websocket.CloseReason;
import javax.websocket.Session;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.smartling.cms.gateway.client.api.model.ResponseStatus;
import com.smartling.cms.gateway.client.command.CommandChannelHandler;
import com.smartling.cms.gateway.client.command.ErrorResponse;
import com.smartling.cms.gateway.client.command.GetHtmlCommand;
import com.smartling.cms.gateway.client.command.GetResourceCommand;
import com.smartling.cms.gateway.client.internal.CommandChannelTransport;
import com.smartling.cms.gateway.client.internal.CommandParser;
import com.smartling.cms.gateway.client.upload.FileUpload;

public class CmsGatewayClientTest
{
    private static final URI STUB_COMMAND_CHANNEL_URI = URI.create("some:command_channel_uri");
    private static final URI STUB_UPLOAD_CHANNEL_URI = URI.create("some_upload_channel_uri?projectId=some+project+id");

    @Mock
    private CommandChannelHandler handler;

    @Mock
    private CommandChannelTransport commandChannel;

    @Mock
    private CloseableHttpAsyncClient uploadChannel;

    @Spy
    private CommandParser commandParser;

    @Mock
    private Future<HttpResponse> futureHttpResponse;

    @Mock
    private Session session;

    private CmsGatewayClient client;


    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        client = new CmsGatewayClient(STUB_COMMAND_CHANNEL_URI, STUB_UPLOAD_CHANNEL_URI, commandChannel, uploadChannel, commandParser);
    }
    
	@Test
	public void opensWebsocketConnectionOnConnect() throws Exception
	{
	    client.connect(handler);

	    verify(commandChannel).connectToServer(anyObject(), eq(STUB_COMMAND_CHANNEL_URI));
	}

    @Test
    public void startsHttpClientOnConnect() throws Exception
    {
        client.connect(handler);

        verify(uploadChannel).start();
    }

    @Test(expected = CmsGatewayClientException.class)
    public void throwsExceptionOnConnectWhenExceptionInCommandChannel() throws Exception
    {
        doThrow(Exception.class).when(commandChannel).connectToServer(any(), any(URI.class));

        client.connect(handler);
    }

    private CmsGatewayClient.CommandChannelTransportEndpoint getCommandChannelTransportEndpoint() throws Exception
    {
        ArgumentCaptor<CmsGatewayClient.CommandChannelTransportEndpoint> captor = ArgumentCaptor.forClass(CmsGatewayClient.CommandChannelTransportEndpoint.class);

        client.connect(handler);

        verify(commandChannel).connectToServer(captor.capture(), any(URI.class));
        return captor.getValue();
    }

    @Test
    public void callsCommandHandlerOnDisconnectWhenTransportClosedNormally() throws Exception
    {
        CloseReason reason = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, null);

        getCommandChannelTransportEndpoint().onClose(null, reason);

        verify(handler, only()).onDisconnect();
    }

    @Test
    public void autoReconnectsCommandChannelWhenTransportClosedAbnormally() throws Exception
    {
        CloseReason reason = new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, null);

        getCommandChannelTransportEndpoint().onClose(null, reason);

        verify(commandChannel, times(2)).connectToServer(anyObject(), eq(STUB_COMMAND_CHANNEL_URI));
        verifyZeroInteractions(handler);
    }

    @Test
    public void disconnectsAfterFailedAttemptToReconnectCommandChannel() throws Exception
    {
        CloseReason reason = new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, null);

        CmsGatewayClient.CommandChannelTransportEndpoint transportEndpoint = getCommandChannelTransportEndpoint();

        doThrow(Throwable.class).when(commandChannel).connectToServer(any(), any(URI.class));

        transportEndpoint.onClose(null, reason);

        verify(handler).onError(any(Throwable.class));
        verify(handler).onDisconnect();
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void forwardsTransportErrorToCommandHandler() throws Exception
    {
        Throwable e = mock(Throwable.class);

        getCommandChannelTransportEndpoint().onError(null, e);

        verify(handler, only()).onError(e);
    }

    @Test
    public void forwardsParserErrorToCommandHandler() throws Exception
    {
        doThrow(Throwable.class).when(commandParser).parse(anyString());

        getCommandChannelTransportEndpoint().onMessage(null, null);

        verify(handler, only()).onError(any(Throwable.class));
    }

    @Test
    public void callsCommandHandlerOnConnectWhenAuthenticatedSuccessfully() throws Exception
    {
        getCommandChannelTransportEndpoint().onMessage("{\"cmd\": \"authenticationSuccess\"}", null);

        verify(handler, only()).onConnect();
    }

    @Test
    public void closesWebsocketAndCallsOnErrorWhenAuthenticationFailed() throws Exception
    {
        getCommandChannelTransportEndpoint().onMessage("{\"cmd\": \"authenticationError\"}", session);

        ArgumentCaptor<CloseReason> reasonCaptor =  ArgumentCaptor.forClass(null);
        verify(session).close(reasonCaptor.capture());
        assertThat(reasonCaptor.getValue().getCloseCode(), is((CloseReason.CloseCode)CloseReason.CloseCodes.NORMAL_CLOSURE));

        verify(handler, only()).onError(any(CmsGatewayClientAuthenticationException.class));
    }

    @Test
    public void closesWebsocketAndCallsOnErrorOnDisconnectCommand() throws Exception
    {
        getCommandChannelTransportEndpoint().onMessage("{\"cmd\": \"disconnect\"}", session);

        ArgumentCaptor<CloseReason> reasonCaptor =  ArgumentCaptor.forClass(null);
        verify(session).close(reasonCaptor.capture());
        assertThat(reasonCaptor.getValue().getCloseCode(), is((CloseReason.CloseCode)CloseReason.CloseCodes.NORMAL_CLOSURE));

        verify(handler, only()).onError(any(CmsGatewayClientException.class));
    }

    @Test
    public void forwardsGetHtmlCommandToCommandHandler() throws Exception
    {
        getCommandChannelTransportEndpoint().onMessage("{\"cmd\":\"getHtml\", \"rid\":\"some request id\", \"uri\":\"some file uri\"}", null);

        GetHtmlCommand command = getGetHtmlCommand();
        assertThat(command.getId(), is("some request id"));
        assertThat(command.getUri(), is("some file uri"));
    }

    private GetHtmlCommand getGetHtmlCommand()
    {
        ArgumentCaptor<GetHtmlCommand> captor = ArgumentCaptor.forClass(null);

        verify(handler, only()).onGetHtmlCommand(captor.capture());

        return captor.getValue();
    }

    @Test
    public void forwardsGetResourceCommandtoCommandHandler() throws Exception
    {
        getCommandChannelTransportEndpoint().onMessage("{\"cmd\":\"getResource\", \"rid\":\"some request id\", \"uri\":\"some file uri\"}", null);

        GetResourceCommand command = getGetResourceCommand();
        assertThat(command.getId(), is("some request id"));
        assertThat(command.getUri(), is("some file uri"));
    }

    private GetResourceCommand getGetResourceCommand()
    {
        ArgumentCaptor<GetResourceCommand> captor = ArgumentCaptor.forClass(null);

        verify(handler, only()).onGetResourceCommand(captor.capture());

        return captor.getValue();
    }

    @Test
    public void sendsErrorResponseJsonToCommandChannel()
    {
        ErrorResponse error = new ErrorResponse(new GetResourceCommand("some request id", "some file uri"));
        String expectedJson = error.toJSONString();

        client.send(error);

        verify(commandChannel).send(expectedJson);
    }

    @Test
    public void postsFileContentsToUploadChannel() throws Exception
    {
        FileUpload response = makeFileUploadResponse("some request id", "some file uri", "some file body");

        client.send(response);

        HttpPost httpPost = getHttpPostFromUploadChannel();
        assertThat(httpPost.getURI(), is(URI.create(STUB_UPLOAD_CHANNEL_URI + "&rid=some+request+id")));
        assertThat(IOUtils.toString(httpPost.getEntity().getContent()), is("some file body"));
    }

    private FileUpload makeFileUploadResponse(String requestId, String fileUri, String bodyText)
    {
        GetResourceCommand request = new GetResourceCommand(requestId, fileUri);
        FileUpload fileUpload = new FileUpload(request);
        fileUpload.setContentStream(IOUtils.toInputStream(bodyText));
        return fileUpload;
    }

    private HttpPost getHttpPostFromUploadChannel()
    {
        ArgumentCaptor<HttpPost> argPost = ArgumentCaptor.forClass(HttpPost.class);
        verify(uploadChannel).execute(argPost.capture(), Mockito.<FutureCallback<HttpResponse>>any());
        return argPost.getValue();
    }

    private HttpResponse mockHttpResponse(int statusCode, String bodyJson)
    {
        HttpResponse response = mock(HttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, "Status " + statusCode));
        HttpEntity entity = EntityBuilder.create()
                .setContentType(ContentType.APPLICATION_JSON)
                .setText(bodyJson)
                .build();
        when(response.getEntity()).thenReturn(entity);
        return response;
    }

    private Future<ResponseStatus<Void>> onUploadResponse(int statusCode, String responseJson) throws Exception
    {
        HttpResponse response = mockHttpResponse(statusCode, responseJson);

        when(futureHttpResponse.get()).thenReturn(response);
        when(uploadChannel.execute(any(HttpUriRequest.class), Mockito.<FutureCallback<HttpResponse>>any())).thenReturn(futureHttpResponse);

        return client.send(makeFileUploadResponse("some request id", "some file uri", "some body content"));
    }

    @Test
    public void returnsSuccessStatusOnSuccessfulUpload() throws Exception
    {
        Future<ResponseStatus<Void>> future = onUploadResponse(200, "{\"response\":{\"code\":\"SUCCESS\"}}");

        assertThat(future.get().getCode(), is("SUCCESS"));
    }

    @Test
    public void returnsErrorStatusOnFailedUpload() throws Exception
    {
        Future<ResponseStatus<Void>> future = onUploadResponse(200, "{\"response\":{\"code\":\"GENERAL_ERROR\",\"messages\":[\"some error message\"]}}");

        assertThat(future.get().getCode(), is("GENERAL_ERROR"));
        assertThat(future.get().getMessages().get(0), is("some error message"));
    }

    @Test
    public void failsOnExceptionInUpload() throws Exception
    {
        Future<ResponseStatus<Void>> future = onUploadResponse(500, "{\"response\":{\"code\":\"GENERAL_ERROR\",\"messages\":[\"some error message\"]}}");

        try
        {
            future.get();
        }
        catch(ExecutionException e)
        {
            CmsGatewayClientException clientException = (CmsGatewayClientException)e.getCause();
            assertThat(clientException.getMessage(), containsString("some error message"));
        }
    }

    @Test
    public void closesCommandChannelOnClose() throws Exception
    {
        client.close();

        verify(uploadChannel).close();
        verify(commandChannel).close();
    }
}
