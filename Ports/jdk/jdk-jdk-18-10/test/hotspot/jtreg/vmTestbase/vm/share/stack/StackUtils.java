/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share.stack;

import java.util.List;
import java.util.Map;

import nsk.share.TestFailure;
import nsk.share.log.Log;

public final class StackUtils {
        private StackUtils() {
        }

        private static String replace(String s) {
                return (s == null || s.length() == 0) ? "?" : s;
        }

        /**
         * String representation of stack trace element.
         *
         * Note that null and empty values are replaced with '?'.
         */
        public static String strStackTraceElement(StackTraceElement element) {
                return "at " + replace(element.getClassName()) + "." + replace(element.getMethodName()) + "(" + replace(element.getFileName()) + ":" + element.getLineNumber() + ")";
        }

        public static void printStackTraceElement(Log log, StackTraceElement element) {
                log.info("       " + strStackTraceElement(element));
        }

        public static void printStackTrace(Log log, StackTraceElement[] elements) {
                for (StackTraceElement element : elements)
                        printStackTraceElement(log, element);
        }

        public static void printStackTrace(Log log, Iterable<StackTraceElement> elements) {
                for (StackTraceElement element : elements)
                        printStackTraceElement(log, element);
        }

        /**
         * Check that element matches expected element.
         *
         * Expected element is used as pattern for matching. A null or empty
         * field value means that no comparison is done.
         */
        public static boolean matches(StackTraceElement element, StackTraceElement expected) {
                return
                        (expected.getClassName() == null || expected.getClassName().length() == 0 || expected.getClassName().equals(element.getClassName())) &&
                        (expected.getMethodName() == null || expected.getMethodName().length() == 0 || expected.getMethodName().equals(element.getMethodName())) &&
                        (expected.isNativeMethod() == element.isNativeMethod());
        }

        public static StackTraceElement expectedTraceElement(String className, String methodName, boolean nativeMethod) {
                // Replace null className with empty because StackTraceElement constructor does not allow null className.
                return new StackTraceElement(className == null ? "" : className, methodName, null, (nativeMethod ? -2 : 0));
        }

        public static void addExpectedTraceElement(List<StackTraceElement> expectedTrace, String className, String methodName, boolean nativeMethod) {
                expectedTrace.add(0, expectedTraceElement(className, methodName, nativeMethod));
        }

        /**
         * Check that trace elements starting from given index match expected elements.
         */
        public static void checkMatches(StackTraceElement[] elements, List<StackTraceElement> expectedTrace, int i) {
                if (elements.length - i < expectedTrace.size())
                        throw new TestFailure("Expected at least " + expectedTrace.size() + " trace elements, got only " + (i + 1));
                for (int j = 0; j < expectedTrace.size(); ++j) {
                        StackTraceElement expected = expectedTrace.get((expectedTrace.size() - 1) - j);
                        int index = (elements.length - 1) - i - j;
                        StackTraceElement actual = elements[index];
                        if (!matches(actual, expected))
                                throw new TestFailure("Expected element at index " + index + " to match: " + strStackTraceElement(expected));
                }
        }

        /**
         * Find matching stack trace element starting from top of the stack.
         */
        public static int findMatch(StackTraceElement[] elements, StackTraceElement expected) {
                for (int i = 0; i < elements.length; ++i)
                        if (StackUtils.matches(elements[elements.length - 1 - i], expected))
                                return i;
                return -1;
        }

        /**
         * Find the stack trace element that contains the "main(String[])" method
         *
         * @return StackTraceElement containing "main" function, null if there are more than one
         */
        public static StackTraceElement findMain() {
                Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
                StackTraceElement mainMethodFrame = null;
                for(StackTraceElement[] current : stackTraces.values()) {
                        if (current.length > 0) {
                                StackTraceElement last = current[current.length - 1];
                                if ("main".equals(last.getMethodName())) {
                                        if (mainMethodFrame == null) {
                                                mainMethodFrame = last;
                                        } else if (!mainMethodFrame.getClassName().equals(last.getClassName())) {
                                                // more than one class has a "main"
                                                return null;
                                        }
                                }
                        }
                }
                return mainMethodFrame;
        }
}
