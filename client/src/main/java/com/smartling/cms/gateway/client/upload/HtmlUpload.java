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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.Validate;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.entity.ContentType;

import com.google.gson.JsonObject;
import com.smartling.cms.gateway.client.command.BaseCommand;
import com.smartling.cms.gateway.client.upload.FileUpload;

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

    public HtmlUpload(BaseCommand request)
    {
        super(request);
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
    protected EntityBuilder getEntityBuilder() throws IOException
    {
        JsonObject response = new JsonObject();
        response.addProperty("baseUrl", baseUrl);
        response.addProperty("body", IOUtils.toString(getInputStream()));

        return EntityBuilder.create()
                .setContentType(ContentType.APPLICATION_JSON)
                .setText(response.toString());
    }
}
