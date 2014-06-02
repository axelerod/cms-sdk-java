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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.junit.Test;

public class HtmlUploadTest
{
    String entityToString(HttpEntity entity) throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        entity.writeTo(os);
        return os.toString();
    }
    private boolean multipartEntityContainsText(HttpEntity entity, String text) throws IOException
    {
        String entityString = entityToString(entity);
        return entityString.contains(text);
    }

    @Test
    public void testHttpEntityDefaultValues() throws Exception
    {
        HtmlUpload response = new HtmlUpload(mock(GetResourceCommand.class));
        response.setContentStream(IOUtils.toInputStream("foo"));
        HttpEntity entity = response.getHttpEntity();
        String sig = "\r\nContent-Disposition: form-data; name=\"file\"\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n";
        assertTrue(multipartEntityContainsText(entity, sig));
    }

    @Test
    public void testHttpEntityHasPartWithMetadataJson() throws Exception
    {
        HtmlUpload response = new HtmlUpload(mock(GetResourceCommand.class));
        response.setContentStream(IOUtils.toInputStream("foo"));
        response.setBaseUrl("base_url_value");
        HttpEntity entity = response.getHttpEntity();
        String metaSig = "\r\nContent-Disposition: form-data; name=\"metadata\"\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n";
        assertTrue(multipartEntityContainsText(entity, metaSig));

        String fileSig = "\r\nContent-Disposition: form-data; name=\"file\"\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n";
        assertTrue(multipartEntityContainsText(entity, fileSig));
    }

    @Test
    public void testHttpEntityMetadataHasBaseUrl() throws Exception
    {
        HtmlUpload response = new HtmlUpload(mock(GetResourceCommand.class));
        response.setContentStream(IOUtils.toInputStream("foo"));
        response.setBaseUrl("base_url_value");
        HttpEntity entity = response.getHttpEntity();
        String metaJson = "\r\n\r\n{\"baseUrl\":\"base_url_value\"}\r\n";
        assertTrue(multipartEntityContainsText(entity, metaJson));
    }
}
