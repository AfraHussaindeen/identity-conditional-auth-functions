/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.conditional.auth.functions.user;

import org.wso2.carbon.identity.application.authentication.framework.config.model.graph.js.JsAuthenticationContext;

/**
 * Function to check if the user in the authentication context has at least one authenticator authenticated for the
 * give step.
 */
@FunctionalInterface
public interface CheckSessionExistenceFunction {

    /**
     * Check if the user in the authentication context is authenticated for the give step.
     *
     * @param step    step number
     * @param context authentication context
     * @return if the user is already authenticated for the step.
     */
    boolean checkSessionExistence(int step, JsAuthenticationContext context);
}
