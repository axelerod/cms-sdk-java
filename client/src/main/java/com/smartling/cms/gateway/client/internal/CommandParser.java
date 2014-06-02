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


import org.apache.commons.lang3.Validate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smartling.cms.gateway.client.CommandBase;

/**
 * Parser of commands, command channel.
 *
 * @author p.ivashkov
 */
public class CommandParser
{
    public CommandBase parse(String value) throws CommandParserException
    {
        Validate.notNull(value);

        Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(CommandBase.class, new CommandTypeAdapter())
            .create();

        try
        {
            CommandBase command = gson.fromJson(value, CommandBase.class);
            Validate.notNull(command);
            return command;
        }
        catch (Throwable e)
        {
            throw new CommandParserException(e);
        }
    }
}
