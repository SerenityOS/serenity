/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.jvm;

import java.nio.file.Paths;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import jdk.jfr.internal.JVM;

/**
 * @test
 * @summary Checks that the JVM can rollback on native initialization failures.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.jfr/jdk.jfr.internal
 * @run main/othervm jdk.jfr.jvm.TestCreateNative
 */
public class TestCreateNative {

    // This is a white-box test where we fabricate a native initialization
    // error by calling JMV#createNative(false), which will tear down
    // all native structures after they are setup, as if something went wrong
    // at the last step.
    public static void main(String... args) throws Exception {
        JVM jvm = JVM.getJVM();
        // Ensure that repeated failures can be handled
        for (int i = 1; i < 4; i++) {
            System.out.println("About to try failed initialization, attempt " + i + " out of 3");
            assertFailedInitialization(jvm);
            System.out.println("As expected, initialization failed.");
        }
        // Ensure that Flight Recorder can be initialized properly after failures
        Configuration defConfig = Configuration.getConfiguration("default");
        Recording r = new Recording(defConfig);
        r.start();
        r.stop();
        r.dump(Paths.get("recording.jfr"));
        r.close();
    }

    private static void assertFailedInitialization(JVM jvm) throws Exception {
        try {
            jvm.createFailedNativeJFR();
            throw new Exception("Expected failure when creating native JFR");
        } catch (IllegalStateException ise) {
            String message = ise.getMessage();
            if (!message.equals("Unable to start Jfr")) {
                throw new Exception("Expected failure on initialization of native JFR");
            }
        }
    }
}
