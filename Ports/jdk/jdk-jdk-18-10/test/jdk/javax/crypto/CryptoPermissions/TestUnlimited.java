/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @test
 * @bug 8061842
 * @summary Package jurisdiction policy files as something other than JAR
 * @run main/othervm TestUnlimited use_default default
 * @run main/othervm TestUnlimited "" exception
 * @run main/othervm TestUnlimited limited limited
 * @run main/othervm TestUnlimited unlimited unlimited
 * @run main/othervm TestUnlimited unlimited/ unlimited
 * @run main/othervm TestUnlimited NosuchDir exception
 * @run main/othervm TestUnlimited . exception
 * @run main/othervm TestUnlimited /tmp/unlimited exception
 * @run main/othervm TestUnlimited ../policy/unlimited exception
 * @run main/othervm TestUnlimited ./unlimited exception
 * @run main/othervm TestUnlimited /unlimited exception
 */
import javax.crypto.*;
import java.security.Security;
import java.nio.file.*;
import java.util.stream.*;

public class TestUnlimited {

    private enum Result {
        UNLIMITED,
        LIMITED,
        EXCEPTION,
        UNKNOWN
    };

    /*
     * Grab the default policy entry from java.security.
     *
     * If the input java.security file is malformed
     * (missing crypto.policy, attribute/no value, etc), throw
     * exception.  split() might throw AIOOB which
     * is ok behavior.
     */
    private static String getDefaultPolicy() throws Exception {
        String javaHome = System.getProperty("java.home");
        Path path = Paths.get(javaHome, "conf", "security", "java.security");

        try (Stream<String> lines = Files.lines(path)) {
            return lines.filter(x -> x.startsWith("crypto.policy="))
                    .findFirst().orElseThrow(
                            () -> new Exception("Missing crypto.policy"))
                    .split("=")[1].trim();
        }
    }

    public static void main(String[] args) throws Exception {
        /*
         * Override the Security property to allow for unlimited policy.
         * Would need appropriate permissions if Security Manager were
         * active.
         */
        if (args.length != 2) {
            throw new Exception("Two args required");
        }

        String testStr = args[0];
        String expectedStr = args[1];
        if (testStr.equals("use_default")) {
            expectedStr = getDefaultPolicy();
        }

        Result expected = Result.UNKNOWN;  // avoid NPE warnings
        Result result;

        switch (expectedStr) {
        case "unlimited":
            expected = Result.UNLIMITED;
            break;
        case "limited":
            expected = Result.LIMITED;
            break;
        case "exception":
            expected = Result.EXCEPTION;
            break;
        default:
            throw new Exception("Unexpected argument");
        }

        System.out.println("Testing: " + testStr);
        if (testStr.equals("\"\"")) {
            Security.setProperty("crypto.policy", "");
        } else {
            // skip default case.
            if (!testStr.equals("use_default")) {
                Security.setProperty("crypto.policy", testStr);
            }
        }

        /*
         * Use the AES as the test Cipher
         * If there is an error initializing, we will never get past here.
         */
        try {
            int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
            System.out.println("max AES key len:" + maxKeyLen);
            if (maxKeyLen > 128) {
                System.out.println("Unlimited policy is active");
                result = Result.UNLIMITED;
            } else {
                System.out.println("Unlimited policy is NOT active");
                result = Result.LIMITED;
            }
        } catch (Throwable e) {
            //ExceptionInInitializerError's
            result = Result.EXCEPTION;
        }

        System.out.println(
                "Expected:\t" + expected + "\nResult:\t\t" + result);
        if (!expected.equals(result)) {
            throw new Exception("Didn't match");
        }

        System.out.println("DONE!");
    }
}
