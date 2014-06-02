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

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.Validate;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import com.google.gson.JsonObject;

/**
 * Response for HTML file upload.
 *
 * @author p.ivashkov
 *
 * {@code
 *  HtmlUpload upload = new HtmlUpload(request);
 *  upload.setFilename("index.html");
 *  upload.setBody(dataString);
 *  client.send(upload);
 * }
 */
public class HtmlUpload extends FileUpload
{
    private String baseUrl;

    public HtmlUpload(GetResourceCommand request)
    {
        super(request);
        setContentType("text/html", CharEncoding.UTF_8);
    }

    public void setBaseUrl(String value)
    {
        baseUrl = value;
    }
    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBody(String value) throws IOException
    {
        Validate.notNull(value);
        setContentStream(IOUtils.toInputStream(value, CharEncoding.UTF_8));
    }

    @Override
    protected MultipartEntityBuilder getEntityBuilder()
    {
        MultipartEntityBuilder builder = super.getEntityBuilder();
        if (baseUrl == null)
            return builder;

        JsonObject meta = new JsonObject();
        meta.addProperty("baseUrl", baseUrl);

        return builder
        .addTextBody("metadata", meta.toString(), ContentType.APPLICATION_JSON);
    }
}
