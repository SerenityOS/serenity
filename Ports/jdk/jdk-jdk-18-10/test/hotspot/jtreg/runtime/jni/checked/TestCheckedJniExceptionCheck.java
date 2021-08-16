/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8164086
 * @summary regression tests for 8164086, verify correct warning from checked JNI
 * @library /test/lib
 * @modules java.management
 * @run main/native TestCheckedJniExceptionCheck launch
 */

import java.util.List;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestCheckedJniExceptionCheck {

    static {
        System.loadLibrary("TestCheckedJniExceptionCheck");
    }

    int callableMethodInvokeCount = 0;

    static final String TEST_START           = "TEST STARTED";
    static final String EXPECT_WARNING_START = "EXPECT_WARNING_START";
    static final String EXPECT_WARNING_END   = "EXPECT_WARNING_END";

    static final String JNI_CHECK_EXCEPTION = "WARNING in native method: JNI call made without checking exceptions when required to from";

    static void printExpectWarningStart(int count) {
        System.out.println(EXPECT_WARNING_START + " " + count);
    }

    static void printExpectWarningEnd() {
        System.out.println(EXPECT_WARNING_END);
    }

    public TestCheckedJniExceptionCheck() {
        initMethodIds("callableMethod", "()V",
                      "callableNestedMethod", "(IZ)V");
        System.out.println(TEST_START);
    }

    public void test() {
        testSingleCallNoCheck();
        testSingleCallCheck();
        testSingleCallNoCheckMultipleTimes();

        testMultipleCallsNoCheck();
        testMultipleCallsCheck();

        testNestedSingleCallsNoCheck();
        testNestedSingleCallsCheck();
        testNestedMultipleCallsNoCheck();
        testNestedMultipleCallsCheck();
    }

    public void testSingleCallNoCheck() {
        System.out.println("testSingleCallNoCheck start");
        callJavaFromNative(1, false);
        System.out.println("testSingleCallNoCheck end");
    }

    public void testSingleCallCheck() {
        System.out.println("testSingleCallCheck start");
        callJavaFromNative(1, true);
        System.out.println("testSingleCallCheck end");
    }

    public void testSingleCallNoCheckMultipleTimes() {
        System.out.println("testSingleCallNoCheckMultipleTimes start");
        callJavaFromNative(1, false);
        callJavaFromNative(1, false);
        System.out.println("testSingleCallNoCheckMultipleTimes end");
    }

    public void testMultipleCallsNoCheck() {
        System.out.println("testMultipleCallsNoCheck start");
        printExpectWarningStart(1);
        callJavaFromNative(2, false);
        printExpectWarningEnd();
        System.out.println("testMultipleCallsNoCheck end");
    }

    public void testMultipleCallsCheck() {
        System.out.println("testMultipleCallsCheck start");
        callJavaFromNative(2, true);
        System.out.println("testMultipleCallsCheck end");
    }

    public void testNestedSingleCallsNoCheck() {
        System.out.println("testNestedSingleCallsNoCheck start");
        callNestedJavaFromNative(1, false);
        System.out.println("testNestedSingleCallsNoCheck end");
    }

    public void testNestedSingleCallsCheck() {
        System.out.println("testNestedSingleCallsCheck start");
        callNestedJavaFromNative(1, true);
        System.out.println("testNestedSingleCallsCheck end");
    }

    public void testNestedMultipleCallsNoCheck() {
        System.out.println("testNestedMultipleCallsNoCheck start");
        printExpectWarningStart(3);
        callNestedJavaFromNative(2, false);
        printExpectWarningEnd();
        System.out.println("testNestedMultipleCallsNoCheck end");
    }

    public void testNestedMultipleCallsCheck() {
        System.out.println("testNestedMultipleCallsCheck start");
        callNestedJavaFromNative(2, true);
        System.out.println("testNestedMultipleCallsCheck end");
    }

    public void callableMethod() {
        callableMethodInvokeCount++;
    }

    public void callableNestedMethod(int nofCalls, boolean withExceptionChecks) {
        callJavaFromNative(nofCalls, withExceptionChecks);
    }

    public native void callJavaFromNative(int nofCalls, boolean withExceptionChecks);

    public native void callNestedJavaFromNative(int nofCalls, boolean withExceptionChecks);

    private native void initMethodIds(String callableMethodName,
                                      String callableMethodSig,
                                      String callableNestedMethodName,
                                      String callableNestedMethodSig);


    // Check warnings appear where they should, with start/end statements in output...
    static void checkOuputForCorrectWarnings(OutputAnalyzer oa) throws RuntimeException {
        oa.shouldHaveExitValue(0);
        List<String> lines = oa.asLines();
        int expectedWarnings = 0;
        int warningCount = 0;
        int lineNo = 0;
        boolean testStartLine = false;
        for (String line : lines) {
            lineNo++;
            if (!testStartLine) { // Skip any warning before the actual test itself
                testStartLine = line.startsWith(TEST_START);
                continue;
            }
            if (line.startsWith(JNI_CHECK_EXCEPTION)) {
                if (expectedWarnings == 0) {
                    oa.reportDiagnosticSummary();
                    throw new RuntimeException("Unexpected warning at line " + lineNo);
                }
                warningCount++;
                if (warningCount > expectedWarnings) {
                    oa.reportDiagnosticSummary();
                    throw new RuntimeException("Unexpected warning at line " + lineNo);
                }
            } else if (line.startsWith(EXPECT_WARNING_START)) {
                String countStr = line.substring(EXPECT_WARNING_START.length() + 1);
                expectedWarnings = Integer.parseInt(countStr);
            } else if (line.startsWith(EXPECT_WARNING_END)) {
                if (warningCount != expectedWarnings) {
                    oa.reportDiagnosticSummary();
                    throw new RuntimeException("Missing warning at line " + lineNo);
                }
                warningCount = 0;
                expectedWarnings = 0;
            }
        }
        if (!testStartLine) {
            throw new RuntimeException("Missing test start line after " + lineNo + " lines");
        }
        /*
        System.out.println("Output looks good...");
        oa.reportDiagnosticSummary();
        */
    }

    public static void main(String[] args) throws Throwable {
        if (args == null || args.length == 0) {
            new TestCheckedJniExceptionCheck().test();
            return;
        }

        // launch and check output
        checkOuputForCorrectWarnings(ProcessTools.executeTestJvm("-Xcheck:jni",
                                                                 "-Djava.library.path=" + Utils.TEST_NATIVE_PATH,
                                                                 "TestCheckedJniExceptionCheck"));
    }

}
