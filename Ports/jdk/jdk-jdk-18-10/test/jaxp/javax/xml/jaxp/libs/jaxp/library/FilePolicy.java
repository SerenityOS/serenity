/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package jaxp.library;

import static jaxp.library.JAXPTestUtilities.getSystemProperty;

import java.io.FilePermission;

import org.testng.ITestContext;

/**
 * This policy can access local XML files.
 */
public class FilePolicy extends BasePolicy {

    @Override
    public void onStart(ITestContext arg0) {
        // suppose to only run othervm mode
        if (isRunWithSecurityManager()) {
            JAXPPolicyManager policyManager = JAXPPolicyManager.getJAXPPolicyManager(true);
            String userdir = getSystemProperty("user.dir");
            policyManager.addPermission(new FilePermission(userdir + "/-", "read,write,delete"));
            String testSrc = System.getProperty("test.src");
            // to handle the directory structure of some functional test suite
            if (testSrc.endsWith("ptests"))
                testSrc = testSrc.substring(0, testSrc.length() - 7);
            policyManager.addPermission(new FilePermission(testSrc + "/-", "read"));
            policyManager.addPermission(new FilePermission(userdir, "read"));
        }
    }
}
