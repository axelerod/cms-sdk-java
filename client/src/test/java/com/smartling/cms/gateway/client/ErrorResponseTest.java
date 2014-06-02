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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;



public class ErrorResponseTest
{
    @Mock private CommandBase request;
    private ErrorResponse response;
    private JsonParser jsonParser;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        when(request.getId()).thenReturn("0000");
        when(request.getUri()).thenReturn("fileuri");
        response = new ErrorResponse(request);
        jsonParser = new JsonParser();
    }

    @Test
    public void testConvertResponseToJsonDefaultValues() throws Exception
    {
        String value = response.toJSONString();
        JsonObject obj = jsonParser.parse(value).getAsJsonObject();
        assertEquals(request.getId(), obj.get("rid").getAsString());
        assertEquals(request.getUri(), obj.get("uri").getAsString());
        assertNull(obj.get("httpCode"));
        assertNull(obj.get("messages"));
    }

    @Test
    public void testConvertResponseToJsonWithHttpCode() throws Exception
    {
        response.setHttpCode(404);
        String value = response.toJSONString();
        JsonObject obj = jsonParser.parse(value).getAsJsonObject();
        assertEquals(404, obj.get("httpCode").getAsInt());
    }

    @Test
    public void testConvertResponseToJsonWithMessages() throws Exception
    {
        response.addErrorMessage("Error text");
        String value = response.toJSONString();

        JsonObject obj = jsonParser.parse(value).getAsJsonObject();
        JsonArray messages = obj.getAsJsonArray("messages");
        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertEquals("Error text", messages.get(0).getAsString());
    }
}
