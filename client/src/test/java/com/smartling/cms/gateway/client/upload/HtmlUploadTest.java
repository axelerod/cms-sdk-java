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
package com.smartling.cms.gateway.client.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartling.cms.gateway.client.command.GetResourceCommand;

public class HtmlUploadTest
{
    private static final String NO_PUBLIC_URL = "";

    @Mock
    private GetResourceCommand resourceCommand;

    @InjectMocks
    private HtmlUpload response;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void setsJsonContentType() throws Exception
    {
        response.setBody("ignored");

        HttpEntity entity = response.getHttpEntity();

        assertEquals("application/json; charset=UTF-8", entity.getContentType().getValue());
    }

    @Test
    public void returnsJsonWithResourceBodyAndBaseUrl() throws Exception
    {
        JsonObject json = responseWith("resource body", "base url", NO_PUBLIC_URL);

        assertEquals("resource body", json.get("body").getAsString());
        assertEquals("base url", json.get("baseUrl").getAsString());
    }

    private JsonObject responseWith(String body, String baseUrl, String publicUrl) throws IOException
    {
        response.setBody(body);
        response.setBaseUrl(baseUrl);
        if (!publicUrl.equals(NO_PUBLIC_URL))
            response.setPublicUrl(publicUrl);

        HttpEntity entity = response.getHttpEntity();

        JsonParser parser = new JsonParser();
        return (JsonObject)parser.parse(IOUtils.toString(entity.getContent()));
    }

    @Test
    public void returnsJsonWithOptionalPublicUrl() throws Exception
    {
        JsonObject json = responseWith("ignore", "ignore", "another url");

        assertEquals("another url", json.get("publicUrl").getAsString());
    }
}
