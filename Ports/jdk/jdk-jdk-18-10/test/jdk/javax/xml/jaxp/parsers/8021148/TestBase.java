/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.security.Policy;

/**
 *
 *
 * @author huizhe.wang@oracle.com
 */
public class TestBase {
    String filePath;
    boolean hasSM;
    String curDir;
    Policy origPolicy;

    String testName;
    static String errMessage;

    int passed = 0, failed = 0;

    /**
     * Creates a new instance of StreamReader
     */
    public TestBase(String name) {
        testName = name;
    }

    //junit @Override
    protected void setUp() {
        if (System.getSecurityManager() != null) {
            hasSM = true;
            System.setSecurityManager(null);
        }

        filePath = System.getProperty("test.src");
        if (filePath == null) {
            //current directory
            filePath = System.getProperty("user.dir");
        }
        origPolicy = Policy.getPolicy();

    }

    //junit @Override
    public void tearDown() {
        // turn off security manager and restore policy
        System.setSecurityManager(null);
        Policy.setPolicy(origPolicy);
        if (hasSM) {
            System.setSecurityManager(new SecurityManager());
        }
        System.out.println("\nNumber of tests passed: " + passed);
        System.out.println("Number of tests failed: " + failed + "\n");

        if (errMessage != null ) {
            throw new RuntimeException(errMessage);
        }
    }

    void fail(String errMsg) {
        if (errMessage == null) {
            errMessage = errMsg;
        } else {
            errMessage = errMessage + "\n" + errMsg;
        }
        failed++;
    }

    void success(String msg) {
        passed++;
        System.out.println(msg);
    }

}
