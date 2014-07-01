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
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.client.HttpAsyncClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.smartling.cms.gateway.client.api.model.ResponseStatus;
import com.smartling.cms.gateway.client.command.AuthenticationErrorCommand;
import com.smartling.cms.gateway.client.command.AuthenticationSuccessCommand;
import com.smartling.cms.gateway.client.command.BaseCommand;
import com.smartling.cms.gateway.client.command.CommandChannelHandler;
import com.smartling.cms.gateway.client.command.ErrorResponse;
import com.smartling.cms.gateway.client.command.GetHtmlCommand;
import com.smartling.cms.gateway.client.command.GetResourceCommand;
import com.smartling.cms.gateway.client.internal.CommandChannelTransport;
import com.smartling.cms.gateway.client.internal.CommandParser;
import com.smartling.cms.gateway.client.upload.FileUpload;

public class CmsGatewayClientTest
{
    @Mock
    private CommandChannelHandler handler;

    @Mock
    private CommandChannelTransport transport;

    @Mock
    private HttpAsyncClient uploadChannel;

    @Mock
    private CommandParser commandParser;

    @Mock
    private CmsGatewayClient.FactoryHelper factoryHelper;

    @Mock
    private Future<HttpResponse> futureHttpResponse;

    @Mock
    private Session session;

    @InjectMocks
    private CmsGatewayClient client;


    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        
        client.setApiKey("api_key");
        client.setProjectId("project_id");

        when(factoryHelper.commandChannelTransport()).thenReturn(transport);
    }
    
    @Test(expected = NullPointerException.class)
    public void checksMissingPropertiesOnGetCommandChannelUri() throws Exception
    {
        CmsGatewayClient client = new CmsGatewayClient();
        // needs apiKey and projectId
        client.getCommandChannelUri();
    }

    @Test
    public void setsDefaultCommandChannelEndpoint() throws Exception
    {
        URI uri = client.getCommandChannelUri();

        assertThat(uri.toString(), startsWith("ws://localhost/cmd/websocket?"));
        assertThat(uri.toString(), containsString("key=api_key"));
        assertThat(uri.toString(), containsString("projectId=project_id"));
    }

    @Test
    public void setsCustomCommandChannelEndpoint() throws Exception
    {
        client.setCommandChannelEndpoint("xx://foobar");
        URI uri = client.getCommandChannelUri();
        assertThat(uri.toString(), startsWith("xx://foobar?"));
    }

    @Test(expected = NullPointerException.class)
    public void validatesParametersOnSetCommandChannelEndpointWhenNull()
    {
        client.setCommandChannelEndpoint(null);
    }

    @Test(expected = NullPointerException.class)
    public void checksMissingPropertiesOnGetUploadChannelUri() throws Exception
    {
        CmsGatewayClient client = new CmsGatewayClient();
        // needs apiKey and projectId
        client.getUploadChannelUri("some request id");
    }

    @Test
    public void setsDefaultUploadChannelEndpoint() throws Exception
    {
        URI uri = client.getUploadChannelUri("rid_value");

        assertThat(uri.toString(), startsWith("http://localhost/upload?"));
        assertThat(uri.toString(), containsString("key=api_key"));
        assertThat(uri.toString(), containsString("projectId=project_id"));
        assertThat(uri.toString(), containsString("rid=rid_value"));
    }

    @Test
    public void setsCustomUploadChannelEndpoint() throws Exception
    {
        client.setUploadChannelEndpoint("xx://foobar");

        URI uri = client.getUploadChannelUri("00");

        assertThat(uri.toString(), startsWith("xx://foobar?"));
    }

    @Test(expected = NullPointerException.class)
    public void validatesParametersOnSetUploadChannelEndpointWhenNull()
    {
        CmsGatewayClient client = new CmsGatewayClient();
        client.setUploadChannelEndpoint(null);
    }

	@Test
	public void connectsWebsocketToOpenCommandChannel() throws Exception
	{
        URI uri = client.getCommandChannelUri();
	    client.openCommandChannel(handler);
	    verify(transport).connectToServer(anyObject(), eq(uri));
	}

	@Test(expected = CmsGatewayClientException.class)
    public void testOpenCommandChannelWhenFailedOnInvoke() throws Exception
    {
        doThrow(Throwable.class).when(transport).connectToServer(any(Class.class), any(URI.class));
        client.openCommandChannel(handler);
    }

    private CmsGatewayClient.CommandChannelTransportEndpoint getTransportEndpoint() throws Exception
    {
        ArgumentCaptor<CmsGatewayClient.CommandChannelTransportEndpoint> captor = ArgumentCaptor.forClass(CmsGatewayClient.CommandChannelTransportEndpoint.class);

        client.openCommandChannel(handler);

        verify(transport).connectToServer(captor.capture(), any(URI.class));
        return captor.getValue();
    }

	@Test(expected = NullPointerException.class)
    public void verifiesParametersToSetCommandHandlerWhenNull() throws Exception
    {
        client.setHandler(null);
    }

	@Test
    public void doesNothingOnConnect() throws Exception
    {
        getTransportEndpoint().onOpen(null, null);
        verifyNoMoreInteractions(handler);
    }

	@Test
    public void callsCommandHandlerOnDisconnectWhenClosedNormally() throws Exception
    {
        CloseReason.CloseCodes closeCode = CloseReason.CloseCodes.NORMAL_CLOSURE;
        CloseReason reason = new CloseReason(closeCode, null);
        getTransportEndpoint().onClose(null, reason);
        verify(handler).onDisconnect();
    }

    @Test
    public void reconnectsOnAbnormalDisconnect() throws Exception
    {
        URI uri = client.getCommandChannelUri();
        // first connectToServer
        CmsGatewayClient.CommandChannelTransportEndpoint transportEndpoint = getTransportEndpoint();

        CloseReason.CloseCodes closeCode = CloseReason.CloseCodes.CLOSED_ABNORMALLY;
        CloseReason reason = new CloseReason(closeCode, null);
        // second connectToServer
        transportEndpoint.onClose(null, reason);

        verify(transport, times(2)).connectToServer(anyObject(), eq(uri));
        verifyZeroInteractions(handler);
    }

    @Test
    public void notifiesHandlerOnAbnormalDisconnectWithErrorReconnecting() throws Exception
    {
        CmsGatewayClient.CommandChannelTransportEndpoint transportEndpoint = getTransportEndpoint();
        verify(transport).connectToServer(any(), any(URI.class));

        CloseReason.CloseCodes closeCode = CloseReason.CloseCodes.PROTOCOL_ERROR;
        CloseReason reason = new CloseReason(closeCode, null);
        doThrow(Throwable.class).when(transport).connectToServer(any(), any(URI.class));

        transportEndpoint.onClose(null, reason);

        verify(handler).onError(any(Throwable.class));
        verify(handler).onDisconnect();
        verifyZeroInteractions(handler);
    }

    @Test
    public void callsCommandHandlerOnError() throws Exception
    {
        Throwable e = mock(Throwable.class);
        getTransportEndpoint().onError(null, e);
        verify(handler).onError(e);
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void callsCommandHandlerOnErrorForCorruptMessage() throws Exception
    {
        doThrow(Throwable.class).when(commandParser).parse(any(String.class));

        getTransportEndpoint().onMessage(any(String.class), null);

        verify(handler).onError(any(Throwable.class));
    }

    private void whenTransportGetsMessage(String message, BaseCommand commandObject) throws Exception
    {
        when(commandParser.parse(anyString())).thenReturn(commandObject);
        getTransportEndpoint().onMessage(message, session);
    }

    @Test
    public void callCommandHandlerOnConnectWhenAuthenticatedSuccessfully() throws Exception
    {
        whenTransportGetsMessage("{\"cmd\": \"authenticationSuccess\"}", new AuthenticationSuccessCommand());

        verify(handler).onConnect();

        verifyNoMoreInteractions(handler);
    }

    @Test
    public void closesAndcallsOnErrorOnAuthenticationErrorCommand() throws Exception
    {
        whenTransportGetsMessage("{\"cmd\": \"authenticationError\"}", new AuthenticationErrorCommand());

        ArgumentCaptor<CloseReason> reasonCaptor =  ArgumentCaptor.forClass(null);
        verify(session).close(reasonCaptor.capture());
        assertThat(reasonCaptor.getValue().getCloseCode(), is((CloseReason.CloseCode)CloseReason.CloseCodes.NORMAL_CLOSURE));

        verify(handler).onError(any(CmsGatewayClientAuthenticationException.class));

        verifyNoMoreInteractions(handler);
    }
    
    @Test
    public void callsHandlerOnGetHtmlCommand() throws Exception
    {
        GetHtmlCommand command = mock(GetHtmlCommand.class);
        when(command.getType()).thenReturn(BaseCommand.Type.GET_HTML);
        when(commandParser.parse(anyString())).thenReturn(command);

        getTransportEndpoint().onMessage(anyString(), null);

        verify(handler).onGetHtmlCommand(command);
    }

    @Test
    public void callsHandlerOnGetResourceCommand() throws Exception
    {
        GetResourceCommand command = mock(GetResourceCommand.class);
        when(command.getType()).thenReturn(BaseCommand.Type.GET_RESOURCE);
        when(commandParser.parse(anyString())).thenReturn(command);

        getTransportEndpoint().onMessage(anyString(), null);

        verify(handler).onGetResourceCommand(command);
    }
    
    @Test
    public void implementsApiForErrorResponse() throws Exception
    {
        GetResourceCommand request = new GetResourceCommand("0000", "fileuri");
        ErrorResponse error = new ErrorResponse(request);
        String jsonString = error.toJSONString();
        client.send(error);
        verify(transport).send(jsonString);
    }

    @Test
    public void implementsApiForFileUpload() throws Exception
    {
        GetResourceCommand request = new GetResourceCommand("0000", "fileuri");
        FileUpload response = mock(FileUpload.class);
        HttpEntity entity = mock(HttpEntity.class);
        when(response.getRequest()).thenReturn(request);
        when(response.getHttpEntity()).thenReturn(entity);

        client.send(response);
        verify(response).getHttpEntity();

        ArgumentCaptor<HttpPost> argPost = ArgumentCaptor.forClass(HttpPost.class);
        verify(uploadChannel).execute(argPost.capture(), Matchers.<FutureCallback<HttpResponse>>any());

        HttpPost post = argPost.getValue();
        assertEquals(client.getUploadChannelUri(request.getId()), post.getURI());
        assertEquals(entity, post.getEntity());
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
        GetResourceCommand request = new GetResourceCommand("0000", "fileuri");
        FileUpload fileUpload = mock(FileUpload.class);
        HttpEntity entity = mock(HttpEntity.class);
        when(fileUpload.getRequest()).thenReturn(request);
        when(fileUpload.getHttpEntity()).thenReturn(entity);

        HttpResponse response = mockHttpResponse(statusCode, responseJson);

        when(futureHttpResponse.get()).thenReturn(response);
        when(uploadChannel.execute(any(HttpUriRequest.class), Mockito.<FutureCallback<HttpResponse>>any())).thenReturn(futureHttpResponse);

        return client.send(fileUpload);
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

        verify(transport).close();
    }
}
