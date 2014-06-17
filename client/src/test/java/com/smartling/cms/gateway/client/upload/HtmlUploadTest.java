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
        response.setBody("resource body");
        response.setBaseUrl("base url");

        HttpEntity entity = response.getHttpEntity();

        JsonParser parser = new JsonParser();
        JsonObject json = (JsonObject)parser.parse(IOUtils.toString(entity.getContent()));

        assertNotNull(json.get("body"));
        assertEquals("resource body", json.get("body").getAsString());

        assertNotNull(json.get("baseUrl"));
        assertEquals("base url", json.get("baseUrl").getAsString());
    }

}
