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

import org.apache.commons.lang3.StringUtils;

/**
 * Holds status code of a response.
 */
public enum Status
{
    SUCCESS(200),
    VALIDATION_ERROR(400),
    AUTHENTICATION_ERROR(401),
    AUTHORIZATION_ERROR(401),
    GENERAL_ERROR(500),
    MAINTENANCE_MODE_ERROR(503),
    INSUFFICIENT_FUNDS(500);

    private int httpCode;

    private Status(int httpCode)
    {
        this.httpCode = httpCode;
    }

    public int getHttpCode()
    {
        return httpCode;
    }

    public static Status findByName(String statusName)
    {
        if (StringUtils.isBlank(statusName))
        {
            return null;
        }

        for (Status status : values())
        {
            if (status.name().equals(statusName))
            {
                return status;
            }
        }

        return null;
    }
}
