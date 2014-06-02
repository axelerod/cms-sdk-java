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

import java.io.InputStream;

import org.apache.commons.lang3.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

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
    private String filename;
    private ContentType contentType = ContentType.APPLICATION_OCTET_STREAM;
    private InputStream contentStream;

    public FileUpload(CommandBase request)
    {
        super(request);
    }

    public void setFilename(String value)
    {
        filename = value;
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

    protected MultipartEntityBuilder getEntityBuilder()
    {
        return MultipartEntityBuilder.create()
        .addBinaryBody("file", contentStream, contentType, filename);
    }

    public HttpEntity getHttpEntity()
    {
        Validate.notNull(contentStream);
        return getEntityBuilder().build();
    }
}
