/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * This class accumulates test results. Test results can be checked with method @{code checkStatus}.
 */
public class TestResult extends TestBase {

    private final List<Info> testCasesInfo;

    public TestResult() {
        testCasesInfo = new ArrayList<>();
    }

    /**
     * Adds new test case info.
     *
     * @param info the information about test case
     */
    public void addTestCase(String info) {
        System.err.println("Test case: " + info);
        testCasesInfo.add(new Info(info));
    }

    public boolean checkEquals(Object actual, Object expected, String message) {
        echo("Testing : " + message);
        if (!Objects.equals(actual, expected)) {
            getLastTestCase().addAssert(String.format("%s\n" +
                    "Expected: %s,\n" +
                    "     Got: %s", message, expected, actual));
            return false;
        }
        return true;
    }

    public boolean checkNull(Object actual, String message) {
        return checkEquals(actual, null, message);
    }

    public boolean checkNotNull(Object actual, String message) {
        echo("Testing : " + message);
        if (Objects.isNull(actual)) {
            getLastTestCase().addAssert(message + " : Expected not null value");
            return false;
        }
        return true;
    }

    public boolean checkFalse(boolean actual, String message) {
        return checkEquals(actual, false, message);
    }

    public boolean checkTrue(boolean actual, String message) {
        return checkEquals(actual, true, message);
    }

    public boolean checkContains(Collection<?> found, Collection<?> expected, String message) {
        Set<?> copy = new HashSet<>(expected);
        copy.removeAll(found);
        if (!found.containsAll(expected)) {
            return checkTrue(false, message + " FAIL : not found elements : " + copy + "\n" +
                    "Actual: " + found);
        } else {
            return checkTrue(true, message + " PASS : all elements found");
        }
    }

    public void addFailure(Throwable th) {
        if (testCasesInfo.isEmpty()) {
            testCasesInfo.add(new Info("Dummy info"));
        }
        getLastTestCase().addFailure(th);
    }

    private Info getLastTestCase() {
        if (testCasesInfo.isEmpty()) {
            throw new IllegalStateException("Test case should be created");
        }
        return testCasesInfo.get(testCasesInfo.size() - 1);
    }

    /**
     * Throws {@code TestFailedException} if one of the checks are failed
     * or an exception occurs. Prints error message of failed test cases.
     *
     * @throws TestFailedException if one of the checks are failed
     *                             or an exception occurs
     */
    public void checkStatus() throws TestFailedException {
        int passed = 0;
        int failed = 0;
        for (Info testCaseInfo : testCasesInfo) {
            if (testCaseInfo.isFailed()) {
                String info = testCaseInfo.info().replace("\n", LINE_SEPARATOR);
                String errorMessage = testCaseInfo.getMessage().replace("\n", LINE_SEPARATOR);
                System.err.printf("Failure in test case:%n%s%n%s%n", info, errorMessage);
                ++failed;
            } else {
                ++passed;
            }
        }
        System.err.printf("Test cases: passed: %d, failed: %d, total: %d.%n", passed, failed, passed + failed);
        if (failed > 0) {
            throw new TestFailedException("Test failed");
        }
        if (passed + failed == 0) {
            throw new TestFailedException("Test cases were not found");
        }
    }

    @Override
    public void printf(String template, Object... args) {
        getLastTestCase().printf(template, args);
    }

    private static class Info {

        private final String info;
        private final StringWriter writer;
        private boolean isFailed;

        private Info(String info) {
            this.info = info;
            writer = new StringWriter();
        }

        public String info() {
            return info;
        }

        public boolean isFailed() {
            return isFailed;
        }

        public void printf(String template, Object... args) {
            writer.write(String.format(template, args));
        }

        public void addFailure(Throwable th) {
            isFailed = true;
            printf("[ERROR] : %s\n", getStackTrace(th));
        }

        public void addAssert(String e) {
            isFailed = true;
            printf("[ASSERT] : %s\n", e);
        }

        public String getMessage() {
            return writer.toString();
        }

        public String getStackTrace(Throwable throwable) {
            StringWriter stringWriter = new StringWriter();
            try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
                throwable.printStackTrace(printWriter);
            }
            return stringWriter.toString();
        }
    }

    public static class TestFailedException extends Exception {
        public TestFailedException(String message) {
            super(message);
        }
    }
}
