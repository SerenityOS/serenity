/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @author Danny Hendler
 * @author Gary Ellison
 * @bug 4244271
 * @summary New policy sometimes has no effect with no  indication given
 * @run main/othervm/policy=setpolicy.jp TestDynamicPolicy
 */

/*
  The test should be given the following permissions:

  grant codeBase "file:testPath" {
        permission java.security.SecurityPermission "setPolicy";
        permission java.security.SecurityPermission "getPolicy";
  };

 */


import java.io.PrintStream;
import java.io.IOException;

import java.lang.System;
import java.security.Policy;


public class TestDynamicPolicy {

    public static void main(String args[]) throws Exception {

        try {
            //
            TestDynamicPolicy jstest = new TestDynamicPolicy();
            jstest.doit();
        } catch(Exception e)  {
            System.out.println("Failed. Unexpected exception:" + e);
            throw e;
        }
        System.out.println("Passed. OKAY");
    }

    private void doit() throws Exception {
        // A security manager must be installed
        SecurityManager sm=System.getSecurityManager();
        if (sm==null)
            throw new
                Exception("Test must be run with a security manager installed");

        // Instantiate and set the new policy
        DynamicPolicy dp = new DynamicPolicy();
        Policy.setPolicy(dp);

        // Verify that policy has been set
        if (dp != Policy.getPolicy())
            throw new Exception("Policy was not set!!");

        // now see this class can access user.name
        String usr = getUserName();

        if (usr != null) {
            System.out.println("Test was able to read user.name prior to refresh!");
            throw new
                Exception("Test was able to read user.name prior to refresh!");
        }

        // Now, make policy allow reading user.name
        dp.refresh();

        // now I should be able to read it
        usr = getUserName();

        if (usr == null) {
            System.out.println("Test was unable to read user.name after refresh!");
            throw new
                Exception("Test was unable to read user.name after refresh!");
        }
        // Now, take away permission to read user.name
        dp.refresh();

        // now I should not be able to read it
        usr = getUserName();

        if (usr != null) {
            System.out.println("Test was able to read user.name following 2nd refresh!");
            throw new
                Exception("Test was able to read user.name following 2nd refresh!");
        }

    }

    private String getUserName() {
        String usr = null;

        try {
            usr = System.getProperty("user.name");
        } catch (Exception e) {
        }
        return usr;
    }
}
