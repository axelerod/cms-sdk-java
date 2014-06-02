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

import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.net.URI;
import java.util.concurrent.Future;

import javax.websocket.CloseReason;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.client.HttpAsyncClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.smartling.cms.gateway.client.internal.CommandChannelTransport;
import com.smartling.cms.gateway.client.internal.CommandParser;
import com.smartling.cms.gateway.client.internal.CommandParserException;

public class CmsGatewayClientTest
{
    private CmsGatewayClient client;
    @Mock private CommandChannelHandler handler;
    @Mock private CommandChannelTransport transport;
    @Mock private HttpAsyncClient uploadChannel;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        
        client = makeClient();
    }
    
    private CmsGatewayClient makeClient()
    {
        CmsGatewayClient client = new CmsGatewayClient();
        client.setApiKey("api_key");
        client.setProjectId("project_id");
        client.setCommandChannelTransport(transport);
        client.setHandler(handler);
        client.setUploadChannel(uploadChannel);
        return client;
    }

    @Test(expected = NullPointerException.class)
    public void checksMissingPropertiesOnGetCommandChannelUri() throws Exception
    {
        CmsGatewayClient client = new CmsGatewayClient();
        assertNull(client.getApiKey());
        assertNull(client.getProjectId());
        client.getCommandChannelUri();
    }

    @Test
    public void setsDefaultCommandChannelEndpoint() throws Exception
    {
        CmsGatewayClient client = new CmsGatewayClient();
        client.setApiKey("api_key");
        client.setProjectId("project_id");
        URI uri = client.getCommandChannelUri();
        assertThat(uri.toString(), startsWith("ws://localhost/cmd/websocket?"));
        assertThat(uri.toString(), containsString("key=api_key"));
        assertThat(uri.toString(), containsString("projectId=project_id"));
    }

    @Test
    public void setsCustomCommandChannelEndpoint() throws Exception
    {
        CmsGatewayClient client = new CmsGatewayClient();
        client.setApiKey("api_key");
        client.setProjectId("project_id");
        client.setCommandChannelEndpoint("xx://foobar");
        URI uri = client.getCommandChannelUri();
        assertThat(uri.toString(), startsWith("xx://foobar?"));
    }

    @Test(expected = NullPointerException.class)
    public void validatesParametersOnSetCommandChannelEndpointWhenNull()
    {
        CmsGatewayClient client = new CmsGatewayClient();
        client.setCommandChannelEndpoint(null);
    }

    @Test(expected = NullPointerException.class)
    public void checksMissingPropertiesOnGetUploadChannelUri() throws Exception
    {
        CmsGatewayClient client = new CmsGatewayClient();
        assertNull(client.getApiKey());
        client.getUploadChannelUri("0");
    }

    @Test
    public void setsDefaultUploadChannelEndpoint() throws Exception
    {
        CmsGatewayClient client = new CmsGatewayClient();
        client.setApiKey("api_key");
        URI uri = client.getUploadChannelUri("rid_value");
        assertThat(uri.toString(), startsWith("http://localhost/upload?"));
        assertThat(uri.toString(), containsString("key=api_key"));
        assertThat(uri.toString(), containsString("rid=rid_value"));
    }

    @Test
    public void setsCustomUploadChannelEndpoint() throws Exception
    {
        CmsGatewayClient client = new CmsGatewayClient();
        client.setApiKey("api_key");
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
    public void testLazyInstantiationOfDefaultCommandChannel() throws Exception
    {
        CmsGatewayClient client = new CmsGatewayClient();
        assertNull(client.getCommandChannelTransport());
        // TODO: verify lazy instantiation
//        client.openCommandChannel(handler);
//        verify(transport).connectToServer(any(Class.class), any(URI.class));
    }

    @Test(expected = NullPointerException.class)
    public void validatesParametersOnSetCommandChannelTransportWhenNull() throws Exception
    {
        client.setCommandChannelTransport(null);
    }

	@Test
	public void testOpenCommandChannelOK() throws Exception
	{
        URI uri = client.getCommandChannelUri();
	    client.openCommandChannel(handler);
	    verify(transport).connectToServer(client.getTransportEndpoint(), uri);
	}

	@Test(expected = CmsGatewayClientException.class)
    public void testOpenCommandChannelWhenFailedOnInvoke() throws Exception
    {
        doThrow(Throwable.class).when(transport).connectToServer(any(Class.class), any(URI.class));
        client.openCommandChannel(handler);
    }

	@Test
    public void testOpenCommandHandlerShouldSetHandler() throws Exception
    {
        client.openCommandChannel(handler);
        client.getTransportEndpoint().onOpen(null, null);
        verify(handler).onConnect();
    }

	@Test(expected = NullPointerException.class)
    public void testSetCommandHandlerWhenNull() throws Exception
    {
        client.setHandler(null);
    }

	@Test
    public void testOnConnectHandler() throws Exception
    {
        client.getTransportEndpoint().onOpen(null, null);
        verify(handler).onConnect();
    }

	@Test
    public void testOnDisconnectHandler() throws Exception
    {
        CloseReason.CloseCodes closeCode = CloseReason.CloseCodes.NORMAL_CLOSURE;
        CloseReason reason = new CloseReason(closeCode, null);
        client.getTransportEndpoint().onClose(null, reason);
        verify(handler).onDisconnect();
    }

    @Test
    public void testKeepAliveOnAbnormalDisconnect() throws Exception
    {
        URI uri = client.getCommandChannelUri();
        // first connectToServer
        client.openCommandChannel(handler);

        CloseReason.CloseCodes closeCode = CloseReason.CloseCodes.CLOSED_ABNORMALLY;
        CloseReason reason = new CloseReason(closeCode, null);
        // second connectToServer
        client.getTransportEndpoint().onClose(null, reason);

        verify(transport, times(2)).connectToServer(client.getTransportEndpoint(), uri);
        verifyZeroInteractions(handler);
    }

    @Test
    public void testKeepAliveOnAbnormalDisconnectWithError() throws Exception
    {
        client.openCommandChannel(handler);
        verify(transport).connectToServer(any(), any(URI.class));

        CloseReason.CloseCodes closeCode = CloseReason.CloseCodes.PROTOCOL_ERROR;
        CloseReason reason = new CloseReason(closeCode, null);
        doThrow(Throwable.class).when(transport).connectToServer(any(), any(URI.class));
        client.getTransportEndpoint().onClose(null, reason);

        verify(handler).onError(any(Throwable.class));
        verify(handler).onDisconnect();
        verifyZeroInteractions(handler);
    }

    @Test
    public void testOnErrorHandler() throws Exception
    {
        Throwable e = mock(Throwable.class);
        client.getTransportEndpoint().onError(null, e);
        verify(handler).onError(e);
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void testInvalidTransportMessageInvokesErrorHandler() throws Exception
    {
        CommandParser parser = mock(CommandParser.class);
        doThrow(Throwable.class).when(parser).parse(any(String.class));
        client.getTransportEndpoint().setCommandParser(parser);
        client.getTransportEndpoint().onMessage(any(String.class), null);
        verify(handler).onError(any(Throwable.class));
    }

    @Test
    public void testCommandWhenBrokenJson() throws Exception
    {
        String message = "{\"cmd\":\"get";
        client.getTransportEndpoint().onMessage(message, null);
        verify(handler).onError(any(CommandParserException.class));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void testUnknownCommand() throws Exception
    {
        String message = "{\"cmd\":\"getAbc\"}";
        client.getTransportEndpoint().onMessage(message, null);
        verify(handler).onError(any(CommandParserException.class));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void callsOnErrorOnAuthenticationErrorCommand() throws Exception
    {
        client.getTransportEndpoint().onMessage("{\"cmd\":\"authenticationError\"}", null);

        verify(handler).onError(any(CmsGatewayClientAuthenticationException.class));

        verifyNoMoreInteractions(handler);
    }
    
    @Test
    public void callsHandlerOnGetHtmlCommand() throws Exception
    {
        client.getTransportEndpoint().onMessage("{\"cmd\":\"getHtml\", \"rid\":\"0000\", \"uri\":\"file_uri\"}", null);
        verify(handler).onGetHtmlCommand(any(GetHtmlCommand.class));
    }

    @Test
    public void callsHandlerOnGetResourceCommand() throws Exception
    {
        client.getTransportEndpoint().onMessage("{\"cmd\":\"getResource\", \"rid\":\"0000\", \"uri\":\"file_uri\"}", null);
        verify(handler).onGetResourceCommand(any(GetResourceCommand.class));
    }
    
    @Test
    public void testErrorResponse() throws Exception
    {
        GetResourceCommand request = new GetResourceCommand("0000", "fileuri");
        ErrorResponse error = new ErrorResponse(request);
        String jsonString = error.toJSONString();
        client.send(error);
        verify(transport).send(jsonString);
    }

    @Test(expected = NullPointerException.class)
    public void testSetUploadChannelWhenNull() throws Exception
    {
        client.setUploadChannel(null);
    }

    @Test
    public void testResponseWithFileUpload() throws Exception
    {
        GetResourceCommand request = new GetResourceCommand("0000", "fileuri");
        FileUpload response = mock(FileUpload.class);
        HttpEntity entity = mock(HttpEntity.class);
        Mockito.when(response.getRequest()).thenReturn(request);
        Mockito.when(response.getHttpEntity()).thenReturn(entity);

        @SuppressWarnings("unused")
        Future<HttpResponse> future = client.send(response);
        verify(response).getHttpEntity();

        ArgumentCaptor<HttpPost> argPost = ArgumentCaptor.forClass(HttpPost.class);
        verify(uploadChannel).execute(argPost.capture(), Matchers.<FutureCallback<HttpResponse>>any());

        HttpPost post = argPost.getValue();
        assertEquals(client.getUploadChannelUri(request.getId()), post.getURI());
        assertEquals(entity, post.getEntity());
    }
}
