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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.google.gson.JsonObject;

/**
 * Base for command class hierarchy, command channel.
 *
 * @author p.ivashkov
 */
public abstract class CommandBase
{
    public static enum Type
    {
        AUTHENTICATION_ERROR(-1),
        GET_HTML(1),
        GET_RESOURCE(2);
        
        private int value;
        
        private Type(int value)
        {
            this.value = value;
        }
        
        public int getValue()
        {
            return value;
        }
    }
    
    private final Type type;
    private final String id;
    private final String uri;

    protected CommandBase(Type type, String id, String uri)
    {
        this.type = type;
        this.id = id;
        this.uri = uri;
    }

    abstract String getCommandName();

    public Type getType()
    {
        return type;
    }
    
    public String getId()
    {
        return id;
    }

    public String getUri()
    {
        return uri;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null || this.getClass() != obj.getClass())
            return false;

        CommandBase command = (CommandBase)obj;
        return new EqualsBuilder()
            .append(getId(), command.getId())
            .append(getUri(), command.getUri())
            .build();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
            .append(getCommandName())
            .append(getId())
            .append(getUri())
            .build();
    }

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    };

    String toJSON()
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("cmd", getCommandName());
        jsonObject.addProperty("rid", getId());
        jsonObject.addProperty("uri", getUri());
        return jsonObject.toString();
    }
}
