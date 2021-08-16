/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.net.URL;
import java.security.Policy;
import java.security.Security;

/*
 * @test
 * @bug 8075706
 * @summary Check that a custom policy provider can be loaded from the classpath
 * @modules java.base/sun.security.provider
 * @run main/othervm -Djava.security.manager=allow UseSystemClassLoader CUSTOM
 * @run main/othervm -Djava.security.manager=allow UseSystemClassLoader DEFAULT
 * @run main/othervm -Djava.security.manager=allow UseSystemClassLoader NOT_AVAIL
 * @run main/othervm -Djava.security.manager=allow UseSystemClassLoader NOT_SET
 */

public class UseSystemClassLoader {

    enum Type {
        CUSTOM, DEFAULT, NOT_AVAIL, NOT_SET
    };

    public static void main(String[] args) throws Exception {

        Type t = Type.valueOf(args[0]);

        // We can't use the jtreg java.security.policy option to specify
        // the policy file because that causes the default JDK policy provider
        // to be set and once set, we cannot change it. So, instead we use the
        // policy.url security property.
        File file = new File(System.getProperty("test.src"), "test.policy");
        URL policyURL = file.toURI().toURL();
        Security.setProperty("policy.url.1", policyURL.toString());

        switch (t) {
            case CUSTOM:
                // Set policy.provider to our custom policy provider
                Security.setProperty("policy.provider", "CustomPolicy");
                break;
            case NOT_AVAIL:
                // Set policy.provider to a non-existent policy provider
                Security.setProperty("policy.provider", "NonExistentPolicy");
                break;
            case DEFAULT:
                // Don't set policy.provider (leave default)
                break;
            case NOT_SET:
                // Set policy.provider to empty string
                Security.setProperty("policy.provider", "");
                break;
        }

        System.setSecurityManager(new SecurityManager());
        Policy p = Policy.getPolicy();
        switch (t) {
            case CUSTOM:
                // check that the custom policy provider has been set
                if (!(p instanceof CustomPolicy)) {
                    throw new Exception("CustomPolicy was not set");
                }
                break;
            case NOT_AVAIL:
            case DEFAULT:
            case NOT_SET:
                // check that the default policy provider has been set
                if (!(p instanceof sun.security.provider.PolicyFile)) {
                    throw new Exception("default provider was not set");
                }
                break;
        }
    }
}
