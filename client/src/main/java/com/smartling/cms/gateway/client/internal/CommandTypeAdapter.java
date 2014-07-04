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
package com.smartling.cms.gateway.client.internal;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.smartling.cms.gateway.client.command.AuthenticationErrorCommand;
import com.smartling.cms.gateway.client.command.AuthenticationSuccessCommand;
import com.smartling.cms.gateway.client.command.BaseCommand;
import com.smartling.cms.gateway.client.command.DisconnectCommand;
import com.smartling.cms.gateway.client.command.GetHtmlCommand;
import com.smartling.cms.gateway.client.command.GetResourceCommand;

/**
 * Factory for command classes, deserializing from JSON string.
 *
 * @author p.ivashkov
 */
public class CommandTypeAdapter implements JsonDeserializer<BaseCommand>
{
    @Override
    public BaseCommand deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonPrimitive prim = jsonObject.getAsJsonPrimitive("cmd");
        String commandName = prim.getAsString();

        if (commandName.equalsIgnoreCase("getResource"))
        {
            String requestId = jsonObject.getAsJsonPrimitive("rid").getAsString();
            String fileUri = jsonObject.getAsJsonPrimitive("uri").getAsString();
            return new GetResourceCommand(requestId, fileUri);
        }

        if (commandName.equalsIgnoreCase("getHtml"))
        {
            String requestId = jsonObject.getAsJsonPrimitive("rid").getAsString();
            String fileUri = jsonObject.getAsJsonPrimitive("uri").getAsString();
            return new GetHtmlCommand(requestId, fileUri);
        }

        if (commandName.equalsIgnoreCase("authenticationSuccess"))
        {
            return new AuthenticationSuccessCommand();
        }

        if (commandName.equalsIgnoreCase("authenticationError"))
        {
            return new AuthenticationErrorCommand();
        }

        if (commandName.equalsIgnoreCase("disconnect"))
        {
            JsonPrimitive reason = jsonObject.getAsJsonPrimitive("message");
            String message = reason == null ? null : reason.getAsString();
            return new DisconnectCommand(message);
        }

        throw new JsonParseException("Unknown command " + commandName);
    }

}
