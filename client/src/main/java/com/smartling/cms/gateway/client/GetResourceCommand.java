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

/**
 * Request command for linked resource.
 *
 * @author p.ivashkov
 */
public class GetResourceCommand extends CommandBase
{
    public GetResourceCommand(String id, String uri)
    {
        super(CommandBase.Type.GET_RESOURCE, id, uri);
    }

    @Override
    String getCommandName()
    {
        return "getResource";
    }
}
