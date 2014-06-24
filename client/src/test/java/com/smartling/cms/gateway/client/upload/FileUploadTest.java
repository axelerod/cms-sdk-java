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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.smartling.cms.gateway.client.command.GetResourceCommand;

public class FileUploadTest
{
    @Mock
    private GetResourceCommand resourceCommand;

    @InjectMocks
    private FileUpload response;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void entityContainsCorrectContentType() throws Exception
    {
        response.setContentStream(new ByteArrayInputStream("ignored".getBytes()));

        response.setContentType("some-content-type", "UTF-8");
        HttpEntity entity = response.getHttpEntity();

        assertEquals("some-content-type; charset=UTF-8", entity.getContentType().getValue());
    }

    @Test
    public void entityContainsResourceBody() throws Exception
    {
        response.setContentStream(new ByteArrayInputStream("body".getBytes()));

        HttpEntity entity = response.getHttpEntity();

        assertEquals("body", IOUtils.toString(entity.getContent()));
    }

    @Test
    public void entityEncodingIsChunkedAsStreamsAreUsed() throws Exception
    {
        response.setContentStream(new ByteArrayInputStream("ignored".getBytes()));

        HttpEntity entity = response.getHttpEntity();

        assertTrue(entity.isChunked());
    }

    @Test(expected = IllegalArgumentException.class)
    public void validatesContentTypeWhenEmpty() throws Exception
    {
        response.setContentType("", "UTF-8");
    }

    @Test
    public void returnsHttpEntityWithDefaultValues() throws Exception
    {
        response.setContentStream(mock(InputStream.class));
        HttpEntity entity = response.getHttpEntity();
        assertNotNull(entity);
    }

    @Test(expected = NullPointerException.class)
    public void throwsWhenMissingContentStream() throws Exception
    {
        response.getHttpEntity();
    }

    @Test(expected = NullPointerException.class)
    public void validatesContentStreamWhenNull() throws Exception
    {
        response.setContentStream(null);
    }
}
