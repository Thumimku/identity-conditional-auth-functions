/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authentication.framework.config.model.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceDataHolder;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.common.testng.WithH2Database;
import org.wso2.carbon.identity.common.testng.WithRealmService;
import org.wso2.carbon.identity.conditional.auth.functions.test.utils.sequence.JsSequenceHandlerAbstractTest;
import org.wso2.carbon.identity.conditional.auth.functions.user.internal.UserFunctionsServiceHolder;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@WithCarbonHome
@WithH2Database(files = "dbscripts/h2.sql")
@WithRealmService(injectToSingletons = {UserFunctionsServiceHolder.class, IdentityTenantUtil.class,
        FrameworkServiceDataHolder.class})
public class HasAnyOfTheRolesFunctionImplTest extends JsSequenceHandlerAbstractTest {

    @WithRealmService
    private RealmService realmService;

    @BeforeMethod
    protected void setUp() throws Exception {

        super.setUp();

        FrameworkServiceDataHolder.getInstance().setAdaptiveAuthenticationAvailable(true);
        sequenceHandlerRunner.registerJsFunction("hasAnyOfTheRoles", new HasAnyOfTheRolesFunctionImpl());
        UserRealm userRealm = realmService.getTenantUserRealm(-1234);
        userRealm.getUserStoreManager().addRole("admin", new String[]{"test_user1", "test_user2"}, null);
        userRealm.getUserStoreManager().addRole("manager", new String[]{"test_user1", "test_user3"}, null);
    }

    @AfterMethod
    public void tearDown() {

    }

    @Test(dataProvider = "hasAnyOfTheRolesDataProvider")
    public void testHasAnyOfTheRoles(String user, boolean steppedUp) throws Exception {

        sequenceHandlerRunner.addSubjectAuthenticator("BasicMockAuthenticator", user, Collections.emptyMap());

        ServiceProvider sp1 = sequenceHandlerRunner.loadServiceProviderFromResource("hasAnyOfTheRoles-test-sp.xml",
                this);

        AuthenticationContext context = sequenceHandlerRunner.createAuthenticationContext(sp1);

        SequenceConfig sequenceConfig = sequenceHandlerRunner
                .getSequenceConfig(context, sp1);
        context.setSequenceConfig(sequenceConfig);
        context.initializeAnalyticsData();

        HttpServletRequest req = sequenceHandlerRunner.createHttpServletRequest();
        HttpServletResponse resp = sequenceHandlerRunner.createHttpServletResponse();

        sequenceHandlerRunner.handle(req, resp, context, "carbon.super");

        HttpServletRequest secondReq = sequenceHandlerRunner.createHttpServletRequest();
        secondReq.setAttribute("s", "S");
        HttpServletResponse secondResp = sequenceHandlerRunner.createHttpServletResponse();

        sequenceHandlerRunner.handle(req, resp, context, "carbon.super");
        assertNotNull(context.getSelectedAcr());
        assertEquals(Boolean.parseBoolean(context.getSelectedAcr()), steppedUp);

    }

    @DataProvider(name = "hasAnyOfTheRolesDataProvider")
    public Object[][] getHasAnyOfTheRolesData() {

        return new Object[][]{
                {"test_user1", true},
                {"test_user2", true},
                {"test_user3", true},
                {"test_user4", false},
        };
    }
}