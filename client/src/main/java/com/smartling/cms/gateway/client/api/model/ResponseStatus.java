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
package com.smartling.cms.gateway.client.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds response status and data.
 */
public class ResponseStatus<D>
{
    private String code;
    private List<String> messages;
    private D data;

    public ResponseStatus(String code, List<String> messages)
    {
        this.code = code;
        this.messages = messages;
    }

    public ResponseStatus(Status status)
    {
        this(status, (String)null);
    }

    public ResponseStatus(Status status, List<String> messages)
    {
        this.code = status.toString();
        this.messages = messages;
    }

    public ResponseStatus(Status status, String message)
    {
        this.code = status.toString();
        if (message != null)
        {
            this.messages = new ArrayList<String>();
            this.messages.add(message);
        }
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getCode()
    {
        return code;
    }

    public void setMessages(List<String> messages)
    {
        this.messages = messages;
    }

    public List<String> getMessages()
    {
        return messages;
    }

    public void setData(D data)
    {
        this.data = data;
    }

    public D getData()
    {
        return data;
    }

}
