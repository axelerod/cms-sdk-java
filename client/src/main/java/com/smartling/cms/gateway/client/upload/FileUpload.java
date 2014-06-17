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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.entity.ContentType;

import com.smartling.cms.gateway.client.Response;
import com.smartling.cms.gateway.client.command.BaseCommand;

/**
 * Response with file upload.
 *
 * @author p.ivashkov
 *
 * {@code
 *  FileUpload upload = new FileUpload(request);
 *  upload.setFilename("style.css");
 *  upload.setContentType("text/css");
 *  upload.setContentStream(dataStream);
 *  client.send(upload);
 * }
 */
public class FileUpload extends Response
{
    private ContentType contentType = ContentType.APPLICATION_OCTET_STREAM;
    private InputStream contentStream;

    public FileUpload(BaseCommand request)
    {
        super(request);
    }

    public void setContentType(String mimeType, String encoding)
    {
        Validate.notNull(mimeType);
        contentType = ContentType.parse(mimeType);
        if (encoding != null) {
            contentType = contentType.withCharset(encoding);
        }
    }

    public void setContentStream(InputStream value)
    {
        Validate.notNull(value);
        contentStream = value;
    }

    protected InputStream getInputStream()
    {
        return this.contentStream;
    }

    protected EntityBuilder getEntityBuilder() throws IOException
    {
        return EntityBuilder.create()
                .setContentType(contentType)
                .chunked()
                .setStream(contentStream);
    }

    public HttpEntity getHttpEntity() throws IOException
    {
        Validate.notNull(contentStream);
        return getEntityBuilder().build();
    }
}
