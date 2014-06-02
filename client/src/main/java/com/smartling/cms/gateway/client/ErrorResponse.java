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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * Error response that client sends back to server.
 *
 * {@code
 *  ErrorResponse error = new ErrorResponse(request);
 *  error.addErrorMessage("Not found");
 *  client.send(error);
 * }
 *
 * @author p.ivashkov
 */
public class ErrorResponse extends Response
{
    private int httpCode;
    private ArrayList<String> messages;

    public ErrorResponse(CommandBase request)
    {
        super(request);
    }

    public int getHttpCode()
    {
        return httpCode;
    }
    public void setHttpCode(int value)
    {
        httpCode = value;
    }

    public void addErrorMessage(String value)
    {
        if (messages == null)
            messages = new ArrayList<String>();
        messages.add(value);
    }

    String toJSONString()
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("state", "error");
        obj.addProperty("rid", getRequest().getId());
        obj.addProperty("uri", getRequest().getUri());
        if (httpCode != 0)
            obj.addProperty("httpCode", new Integer(httpCode));
        if (messages != null)
        {
            Type typeOfSrc = new TypeToken<Collection<String>>(){}.getType();
            Gson gson = new Gson();
            JsonElement jsonArray = gson.toJsonTree(messages, typeOfSrc);
            obj.add("messages", jsonArray);
        }
        return obj.toString();
    }

}
