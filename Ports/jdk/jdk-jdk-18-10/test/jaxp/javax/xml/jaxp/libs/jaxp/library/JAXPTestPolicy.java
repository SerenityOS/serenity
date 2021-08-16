/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.net.SocketPermission;
import org.testng.ITestContext;

/**
 * Covers all policies currently required for running JAXP tests
 */
public class JAXPTestPolicy extends BasePolicy {
    @Override
    public void onStart(ITestContext arg0) {
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

            policyManager.addPermission(new RuntimePermission("accessClassInPackage.com.sun.org.apache.xerces.internal.jaxp"));
            policyManager.addPermission(new RuntimePermission("accessClassInPackage.com.sun.org.apache.xerces.internal.impl"));
            policyManager.addPermission(new RuntimePermission("accessClassInPackage.com.sun.org.apache.xerces.internal.xni.parser"));
            policyManager.addPermission(new RuntimePermission("accessClassInPackage.com.sun.org.apache.bcel.internal.classfile"));
            policyManager.addPermission(new RuntimePermission("accessClassInPackage.com.sun.org.apache.bcel.internal.generic"));
            policyManager.addPermission(new RuntimePermission("accessClassInPackage.com.sun.org.apache.xalan.internal.xsltc.trax"));
            policyManager.addPermission(new RuntimePermission("accessClassInPackage.com.sun.xml.internal.stream"));

            policyManager.addPermission(new SocketPermission("openjdk.java.net:80", "connect,resolve"));
            policyManager.addPermission(new SocketPermission("www.w3.org:80", "connect,resolve"));
        }
    }
}
