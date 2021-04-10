/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.identity.conditional.auth.functions.user;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.config.model.graph.js.JsAuthenticatedUser;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.user.profile.mgt.UserProfileException;
import org.wso2.carbon.identity.user.profile.mgt.dao.UserProfileMgtDAO;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class SetAccountAssociationToLocalUserImpl implements SetAccountAssociationToLocalUser {

    private static final Log log = LogFactory.getLog(SetAccountAssociationToLocalUserImpl.class);

    @Override
    public void doAssociationWithLocalUser(JsAuthenticatedUser federatedUser, String username, String tenantDomain,
                                           String userStoreDomainName) {

        String federatedIdpName = null;
        try {
            if (federatedUser != null) {
                if (federatedUser.getWrapped().isFederatedUser()) {
                    federatedIdpName = federatedUser.getWrapped().getFederatedIdPName();
                    String userIdClaimURI = getUserIdClaimURI(federatedIdpName);
                    String externalSubject;
                    if (StringUtils.isNotEmpty(userIdClaimURI)) {
                        externalSubject = federatedUser.getWrapped().getUserAttributes().entrySet().stream().filter(
                                userAttribute -> userAttribute.getKey().getRemoteClaim().getClaimUri()
                                        .equals(userIdClaimURI))
                                .map(Map.Entry::getValue)
                                .findFirst()
                                .orElse(null);
                    } else {
                        externalSubject = federatedUser.getWrapped().getAuthenticatedSubjectIdentifier();
                    }
                    String externalIdpName = federatedUser.getWrapped().getFederatedIdPName();
                    if (externalSubject != null && externalIdpName != null) {
                        associateID(externalIdpName, externalSubject, username, tenantDomain, userStoreDomainName);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug(" Authenticated user or External IDP may be null " + " Authenticated User: " +
                                    externalSubject + " and the External IDP name: " + externalIdpName);
                        }
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("User " + federatedUser.getWrapped().getAuthenticatedSubjectIdentifier() + " " +
                                "is not a federated user.");
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(" Federated user is null ");
                }
            }
        } catch (IdentityProviderManagementException e) {
            String msg =
                    "Error while retrieving identity provider by name: " + federatedIdpName;
            log.error(msg, e);
        }
    }

    private String getUserIdClaimURI(String federatedIdpName) throws IdentityProviderManagementException {

        String userIdClaimURI = null;
        String userNameLocalClaim = "http://wso2.org/claims/username";
        IdentityProviderManagementService idpManager = new IdentityProviderManagementService();
        IdentityProvider idp = idpManager.getIdPByName(federatedIdpName);
        if (idp == null) {
            return null;
        }
        ClaimConfig claimConfigs = idp.getClaimConfig();
        if (claimConfigs == null) {
            return null;
        }
        ClaimMapping[] claimMappings = claimConfigs.getClaimMappings();
        if (CollectionUtils.isEmpty(Collections.singleton(userIdClaimURI))) {
            return null;
        }
        ClaimMapping userNameClaimMapping = Arrays.stream(claimMappings).filter(claimMapping ->
                StringUtils.equals(userNameLocalClaim, claimMapping.getLocalClaim().getClaimUri()))
                .findFirst()
                .orElse(null);
        if (userNameClaimMapping != null) {
            userIdClaimURI = userNameClaimMapping.getRemoteClaim().getClaimUri();
        }
        return userIdClaimURI;
    }

    /**
     * @param idpID               external IDP name
     * @param associatedID        external authenticated user
     * @param username            local user name
     * @param tenantDomain        tenant domain
     * @param userStoreDomainName user store domain name
     */
    private void associateID(String idpID, String associatedID, String username, String tenantDomain,
                             String userStoreDomainName) {

        int tenantID = IdentityTenantUtil.getTenantId(tenantDomain);
        try {
            UserProfileMgtDAO userProfileMgtDAO = UserProfileMgtDAO.getInstance();
            userProfileMgtDAO.createAssociation(tenantID, userStoreDomainName, username, idpID, associatedID);
        } catch (UserProfileException e) {
            String msg = "Error while creating association for user: " + username + " with federated IdP: " + "" + idpID;
            log.error(msg, e);
        }
    }
}
