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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.http.HttpEntity;
import org.junit.Test;

public class FileUploadTest
{
    @Test
    public void testHttpEntityIsMultiPart() throws Exception
    {
        FileUpload response = new FileUpload(mock(GetResourceCommand.class));
        response.setContentStream(mock(InputStream.class));
        HttpEntity entity = response.getHttpEntity();
        assertTrue(entity.getContentType().getValue().startsWith("multipart/form-data"));
    }

    private boolean multipartEntityContainsText(HttpEntity entity, String text) throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        entity.writeTo(os);
        String entityString = os.toString();
        return entityString.contains(text);
    }

    @Test
    public void testHttpEntityHasFilePart() throws Exception
    {
        FileUpload response = new FileUpload(mock(GetResourceCommand.class));
        response.setContentStream(IOUtils.toInputStream("foo"));
        HttpEntity entity = response.getHttpEntity();
        String sig = "\r\nContent-Disposition: form-data; name=\"file\"\r\n" +
                "Content-Type: application/octet-stream\r\n";
        assertTrue(multipartEntityContainsText(entity, sig));
    }

    @Test
    public void testHttpEntityFilePartHasFilename() throws Exception
    {
        FileUpload response = new FileUpload(mock(GetResourceCommand.class));
        response.setContentStream(IOUtils.toInputStream("foo"));
        response.setFilename("file_name");
        HttpEntity entity = response.getHttpEntity();
        String sig = "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"file_name\"\r\n";
        assertTrue(multipartEntityContainsText(entity, sig));
    }

    @Test
    public void testHttpEntityFilePartHasContentType() throws Exception
    {
        FileUpload response = new FileUpload(mock(GetResourceCommand.class));
        response.setContentStream(IOUtils.toInputStream("foo"));
        response.setContentType("text/plain", CharEncoding.UTF_8);
        HttpEntity entity = response.getHttpEntity();
        String sig = "\r\nContent-Disposition: form-data; name=\"file\"\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n";
        assertTrue(multipartEntityContainsText(entity, sig));
    }

    @Test(expected = NullPointerException.class)
    public void testSetContentTypeWhenMimeTypeIsNull() throws Exception
    {
        FileUpload response = new FileUpload(mock(GetResourceCommand.class));
        response.setContentType(null, "");
    }

    @Test
    public void testSetContentTypeWithDefaultEncoding() throws Exception
    {
        FileUpload response = new FileUpload(mock(GetResourceCommand.class));
        response.setContentType("text/plain", null);
    }

    @Test
    public void testGetHttpEntityWithDefaultValues() throws Exception
    {
        FileUpload response = new FileUpload(mock(GetResourceCommand.class));
        response.setContentStream(mock(InputStream.class));
        HttpEntity entity = response.getHttpEntity();
        assertNotNull(entity);
    }

    @Test(expected = NullPointerException.class)
    public void testGetHttpEntityWhenMissingContentStream() throws Exception
    {
        FileUpload response = new FileUpload(mock(GetResourceCommand.class));
        response.getHttpEntity();
    }

    @Test(expected = NullPointerException.class)
    public void testSetContentStreamWhenNull() throws Exception
    {
        FileUpload response = new FileUpload(mock(GetResourceCommand.class));
        response.setContentStream(null);
    }
}
