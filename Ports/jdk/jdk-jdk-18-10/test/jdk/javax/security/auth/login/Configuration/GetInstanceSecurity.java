/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 6268315
 * @modules jdk.security.auth
 * @summary Configuration should be provider-based
 * @build GetInstanceConfigSpi GetInstanceProvider
 * @run main/othervm/policy=GetInstanceSecurity.policy GetInstanceSecurity
 */

import java.io.File;
import java.net.URI;
import java.security.Policy;
import java.security.Security;
import java.security.URIParameter;
import javax.security.auth.login.Configuration;

public class GetInstanceSecurity {

    private static final String JAVA_CONFIG = "JavaLoginConfig";

    public static void main(String[] args) throws Exception {
        try {
            Configuration c = Configuration.getInstance(JAVA_CONFIG, null);
            throw new RuntimeException("did not catch security exception");
        } catch (SecurityException se) {
            // good
        }

        try {
            Configuration c = Configuration.getInstance
                        (JAVA_CONFIG, null, "SUN");
            throw new RuntimeException("did not catch security exception");
        } catch (SecurityException se) {
            // good
        }

        try {
            Configuration c = Configuration.getInstance
                        (JAVA_CONFIG, null, Security.getProvider("SUN"));
            throw new RuntimeException("did not catch security exception");
        } catch (SecurityException se) {
            // good
        }

        // set a new policy that grants the perms, and then re-check perms

        File file = new File(System.getProperty("test.src", "."),
                                "GetInstanceSecurity.grantedPolicy");
        URI uri = file.toURI();
        URIParameter param = new URIParameter(uri);
        Policy p = Policy.getInstance("JavaPolicy", param, "SUN");
        Policy.setPolicy(p);

        // retry operations

        file = new File(System.getProperty("test.src", "."),
                        "GetInstance.config");
        URIParameter uriParam = new URIParameter(file.toURI());

        try {
            Configuration c = Configuration.getInstance(JAVA_CONFIG, uriParam);
        } catch (SecurityException se) {
            throw new RuntimeException("unexpected SecurityException");
        }

        try {
            Configuration c = Configuration.getInstance
                        (JAVA_CONFIG, uriParam, "SUN");
            // good
        } catch (SecurityException se) {
            throw new RuntimeException("unexpected SecurityException");
        }

        try {
            Configuration c = Configuration.getInstance
                        (JAVA_CONFIG, uriParam, Security.getProvider("SUN"));
            // good
        } catch (SecurityException se) {
            throw new RuntimeException("unexpected SecurityException");
        }

        System.out.println("test passed");
    }
}
