/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4947618
 * @summary Recursion problem in security manager and policy code
 *
 * @run main/othervm/policy=Recursion.policy Recursion
 */

import java.net.*;
import java.security.*;

public class Recursion {

    public static void main(String[] args) throws Exception {

        // trigger security check to make sure policy is set
        try {
            System.getProperty("foo.bar");
        } catch (Exception e) {
            // fall thru
        }

        // static perms
        Permissions staticPerms = new Permissions();
        staticPerms.add(new java.util.PropertyPermission("static.foo", "read"));

        ProtectionDomain pd = new ProtectionDomain
                        (new CodeSource
                                (new URL("http://foo"),
                                (java.security.cert.Certificate[])null),
                        staticPerms,
                        null,
                        null);

        // test with no SecurityManager
        //
        // merging should have occured - check for policy merged.foo permission

        System.setSecurityManager(null);
        if (pd.toString().indexOf("merged.foo") < 0) {
            throw new Exception("Test without SecurityManager failed");
        }

        // test with SecurityManager on the bootclasspath, debug turned off,
        // getPolicyPermission granted
        //
        // merging should have occured - check for policy merged.foo permission

        ProtectionDomain pd2 = new ProtectionDomain
                        (new CodeSource
                                (new URL("http://bar"),
                                (java.security.cert.Certificate[])null),
                        staticPerms,
                        null,
                        null);

        System.setSecurityManager(new SecurityManager());
        if (pd2.toString().indexOf("merged.foo") < 0) {
            throw new Exception("Test with bootclass SecurityManager failed");
        }
    }
}
