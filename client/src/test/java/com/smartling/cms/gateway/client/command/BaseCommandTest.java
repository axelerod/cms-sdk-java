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
package com.smartling.cms.gateway.client.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BaseCommandTest
{

    @Test
    public void testCommandEqualsImplemented()
    {
        FooCommand a = new FooCommand("x", "y");
        FooCommand b = new FooCommand("x", "y");
        FooCommand c = new FooCommand("x", "z");
        assertEquals(b, a);
        assertNotEquals(a, c);
    }

    @Test
    public void testCommandNotEqualsBetweenSubclasses() throws Exception
    {
        FooCommand a = new FooCommand("x", "y");
        BarCommand b = new BarCommand("x", "y");
        assertNotEquals(a, b);
    }

    @Test
    public void testCommandHashcodeImplemented() throws Exception
    {
        FooCommand a = new FooCommand("x", "y");
        FooCommand b = new FooCommand("x", "y");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testCommandHashcodeDiffersBetweenSubclasses() throws Exception
    {
        FooCommand a = new FooCommand("x", "y");
        BarCommand b = new BarCommand("x", "y");
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testCommandToJsonSerialization() throws Exception
    {
        FooCommand foo = new FooCommand("x", "y");
        String json = foo.toJSON();

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(json).getAsJsonObject();
        assertEquals("foo", jsonObject.getAsJsonPrimitive("cmd").getAsString());
        assertEquals("x", jsonObject.getAsJsonPrimitive("rid").getAsString());
        assertEquals("y", jsonObject.getAsJsonPrimitive("uri").getAsString());
    }

    private static class FooCommand extends BaseCommand
    {
        public FooCommand(String id, String uri)
        {
            super(BaseCommand.Type.GET_RESOURCE, id, uri);
        }

        @Override
        String getCommandName()
        {
            return "foo";
        }
    }

    private static class BarCommand extends BaseCommand
    {
        public BarCommand(String id, String uri)
        {
            super(BaseCommand.Type.GET_RESOURCE, id, uri);
        }

        @Override
        String getCommandName()
        {
            return "bar";
        }
    }
}
