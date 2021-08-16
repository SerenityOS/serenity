/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 */
import java.io.File;

/**
 * Setup static variables to represent properties in test environment.
 */
public class TestParams {

    /** variables that hold value property values */
    public static final String testSrc;
    public static final String testClasses;
    public static final String testClassPath;

    /** name of default security policy for test JVM */
    public static final String defaultPolicy;

    /** name of default security policy for RegistryVM */
    public static final String defaultRegistryPolicy;

    /** name of default security manager */
    public static final String defaultSecurityManager;

    /** VM options string */
    public static final String testVmOpts;

    /** Java options string */
    public static final String testJavaOpts;

    /* Initalize commonly used strings */
    static {
        testSrc = TestLibrary.getProperty("test.src", ".");
        testClasses = TestLibrary.getProperty("test.classes", ".");
        testClassPath = TestLibrary.getProperty("test.class.path", ".");

        String dp = TestLibrary.getProperty("java.security.policy", null);
        if (dp == null) {
            dp = testSrc + File.separatorChar + "security.policy";
        }
        defaultPolicy = dp;

        defaultRegistryPolicy =
            testSrc + File.separatorChar + "registry.security.policy";

        String tmp = TestLibrary.getProperty("java.security.manager", null);
        if (tmp == null || tmp.equals("allow")) {
            tmp = "java.lang.SecurityManager";
        }
        defaultSecurityManager = tmp;

        testVmOpts = TestLibrary.getProperty("test.vm.opts", "");

        testJavaOpts = TestLibrary.getProperty("test.java.opts", "");
    }
}
