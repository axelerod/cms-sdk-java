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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.smartling.cms.gateway.client.command.AuthenticationErrorCommand;

import org.junit.Before;
import org.junit.Test;

import com.smartling.cms.gateway.client.command.BaseCommand;
import com.smartling.cms.gateway.client.command.GetHtmlCommand;
import com.smartling.cms.gateway.client.command.GetResourceCommand;

public class CommandParserTest
{
    CommandParser commandParser;

    @Before
    public void setup()
    {
        commandParser = new CommandParser();
    }

    @Test
    public void testParseMessageReturnsHtmlCommandRequest() throws Exception
    {
        String message = "{\"cmd\":\"getHtml\", \"uri\":\"fileuri\", \"rid\":\"0000\"}";
        BaseCommand request = commandParser.parse(message);

        assertThat(request, instanceOf(GetHtmlCommand.class));
        assertThat(request.getId(), is("0000"));
        assertThat(request.getUri(), is("fileuri"));
    }

    @Test
    public void testParseMessageReturnsResourceCommandRequest() throws Exception
    {
        String message = "{\"cmd\":\"getResource\", \"uri\":\"fileuri\", \"rid\":\"0000\"}";
        BaseCommand request = commandParser.parse(message);

        assertThat(request, instanceOf(GetResourceCommand.class));
        assertThat(request.getId(), is("0000"));
        assertThat(request.getUri(), is("fileuri"));
    }

    @Test
    public void throwsParseAuthenticationErrorCommand() throws Exception
    {
        BaseCommand command = commandParser.parse("{\"cmd\": \"authenticationError\"}");

        assertThat(command, instanceOf(AuthenticationErrorCommand.class));
    }

    @Test(expected = NullPointerException.class)
    public void testParseErrorWhenNull() throws Exception
    {
        commandParser.parse(null);
    }

    @Test(expected = CommandParserException.class)
    public void testParseErrorForUnknownCommandRequest() throws Exception
    {
        String message = "{\"cmd\":\"foobar\"}";
        commandParser.parse(message);
    }

    @Test(expected = CommandParserException.class)
    public void testParseErrorWhenCmdNull() throws Exception
    {
        String message = "{\"cmd\":null}";
        commandParser.parse(message);
    }

    @Test(expected = CommandParserException.class)
    public void testParseErrorForEmptyJsonObject() throws Exception
    {
        String message = "{}";
        commandParser.parse(message);
    }

    @Test(expected = CommandParserException.class)
    public void testParseErrorOnCorruptJson() throws Exception
    {
        String message = "{foobar";
        commandParser.parse(message);
    }

    @Test(expected = CommandParserException.class)
    public void testParseErrorForUnexpectedJson_whenArray() throws Exception
    {
        String message = "[\"foobar\"]";
        commandParser.parse(message);
    }

    @Test(expected = CommandParserException.class)
    public void testParseErrorForUnexpectedJson_whenNull() throws Exception
    {
        String message = "null";
        commandParser.parse(message);
    }
}
