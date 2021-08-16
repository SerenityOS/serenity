/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

package MyPackage;

/**
 * @test
 * @summary Basic test for JVMTI AddModuleUses and AddModuleProvides
 * @requires vm.jvmti
 * @build java.base/java.lang.TestProvider
 *        java.base/jdk.internal.test.TestProviderImpl
 * @compile AddModuleUsesAndProvidesTest.java
 * @run main/othervm/native -agentlib:AddModuleUsesAndProvidesTest MyPackage.AddModuleUsesAndProvidesTest
 */

import java.io.PrintStream;
import java.lang.TestProvider;

public class AddModuleUsesAndProvidesTest {

    static {
        try {
            System.loadLibrary("AddModuleUsesAndProvidesTest");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load AddModuleUsesAndProvidesTest library");
            System.err.println("java.library.path: "
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static int checkUses(Module baseModule, Class<?> service);
    native static int checkProvides(Module baseModule, Class<?> service, Class<?> serviceImpl);

    public static void main(String args[]) throws Exception {
        Module baseModule = Object.class.getModule();
        Class<?> service = TestProvider.class;
        Class<?> serviceImpl = Class.forName("jdk.internal.test.TestProviderImpl");

        System.out.println("\n*** Checks for JVMTI AddModuleUses ***\n");

        int status = checkUses(baseModule, service);
        if (status != 0) {
            throw new RuntimeException("Non-zero status returned from the agent: " + status);
        }

        System.out.println("\n*** Checks for JVMTI AddModuleProvides ***\n");

        System.out.println("Check #PC1:");
        if (TestProvider.providers().iterator().hasNext()) {
            throw new RuntimeException("Check #PC1: Unexpectedly service is provided");
        }

        status = checkProvides(baseModule, service, serviceImpl);
        if (status != 0) {
            throw new RuntimeException("Non-zero status returned from the agent: " + status);
        }

        System.out.println("Check #PC3:");
        if (!TestProvider.providers().iterator().hasNext()) {
            throw new RuntimeException("Check #PC3: Unexpectedly service is not provided");
        }
    }
}
