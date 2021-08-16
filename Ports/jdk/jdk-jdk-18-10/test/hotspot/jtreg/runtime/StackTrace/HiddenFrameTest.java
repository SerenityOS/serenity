/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8216977
 * @summary Test null source file and negative line number from hidden frame produces correct output.
 * @library /test/lib
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+ShowHiddenFrames HiddenFrameTest visible
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:-ShowHiddenFrames HiddenFrameTest hidden
 */

import jdk.test.lib.Asserts;

public class HiddenFrameTest {

    @FunctionalInterface
    private static interface SomeFunctionalInterface {
        String someMethod(String a, String b);
    }

    static void assertContains(String expected, String actual, Exception e, boolean framesAreHidden) throws Exception {
        if (!framesAreHidden && !actual.contains(expected)) {
            throw new RuntimeException("Expected: " + expected + "; Actual: " + actual, e);
        } else if (framesAreHidden && actual.contains(expected)) {
            throw new RuntimeException("Unexpected: " + expected + "; Actual: " + actual, e);
        }
    }

    static void checkException(Exception e, boolean framesAreHidden) throws Exception {
        StackTraceElement[] fs = e.getStackTrace();

        if (fs.length < 2) {
            throw new RuntimeException("Exception should have at least two frames", e);
        }

        assertContains("someMethod(Unknown Source)", fs[0].toString(), e, framesAreHidden);
    }

    public static void main(String[] args) throws Exception {
        boolean framesAreHidden = false;
        if (args.length > 0) {
            String arg = args[0];
            if (arg.equals("hidden")) framesAreHidden = true;
        }

        try {
            final SomeFunctionalInterface concatter = String::concat;
            final String nullString = null;
            if (concatter != null) {
                // This throws NPE from the lambda expression which is a hidden frame
                concatter.someMethod(nullString, "validString");
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            checkException(e, framesAreHidden);
        }
    }
}

