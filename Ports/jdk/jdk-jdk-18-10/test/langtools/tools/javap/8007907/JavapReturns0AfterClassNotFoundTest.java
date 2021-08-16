/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8007907
 * @summary javap, method com.sun.tools.javap.Main.run returns 0 even in case
 * of class not found error
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class JavapReturns0AfterClassNotFoundTest {

    static final String fileNotFoundErrorMsg =
            "Error: class not found: Unexisting.class";
    static final String exitCodeClassNotFoundAssertionMsg =
            "Javap's exit code for class not found should be 1";
    static final String classNotFoundMsgAssertionMsg =
            "Javap's error message for class not found is incorrect";

    public static void main(String args[]) throws Exception {
        new JavapReturns0AfterClassNotFoundTest().run();
    }

    void run() throws IOException {
        check(exitCodeClassNotFoundAssertionMsg, classNotFoundMsgAssertionMsg,
                fileNotFoundErrorMsg, "-v", "Unexisting.class");
    }

    void check(String exitCodeAssertionMsg, String errMsgAssertionMsg,
            String expectedErrMsg, String... params) {
        int result;
        StringWriter s;
        String out;
        try (PrintWriter pw = new PrintWriter(s = new StringWriter())) {
            result = com.sun.tools.javap.Main.run(params, pw);
            //no line separator, no problem
            out = s.toString().trim();
        }
        if (result != 1) {
            System.out.println("actual exit code " + result);
            throw new AssertionError(exitCodeAssertionMsg);
        }

        if (!out.equals(expectedErrMsg)) {
            System.out.println("actual " + out);
            System.out.println("expected " + expectedErrMsg);
            throw new AssertionError(errMsgAssertionMsg);
        }
    }

}
