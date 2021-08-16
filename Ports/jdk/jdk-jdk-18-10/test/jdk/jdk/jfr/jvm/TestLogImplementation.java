/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.jfr.internal.JVM;
import jdk.jfr.internal.Logger;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.LogLevel;

/**
 * @test TestLogImplementation
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.jfr/jdk.jfr.internal
 * @run main/othervm jdk.jfr.jvm.TestLogImplementation
 */
public class TestLogImplementation {
    private static LogLevel DEFAULT_TEST_LOG_LEVEL = LogLevel.ERROR;
    private static LogTag JFR_LOG_TAG = LogTag.JFR;

    private static void assertAllLevelsForTag(LogTag tag, String message) {
        for (LogLevel level : LogLevel.values()) {
            Logger.log(tag, level, message);
        }
    }

    private static void assertAllTagsForLevel(LogLevel level, String message) {
        for (LogTag tag : LogTag.values()) {
            Logger.log(tag, level, message);
        }
    }

    public static void main(String... args) {
        assertEmptyMessageOK();
        assertNullMessageOK();
        assertAllCharsOK();
        assertLargeMessageOK();
        assertFormatSpecifierOK();
        assertAllLevelOK();
        assertLogLevelUnderflow();
        assertLogLevelOverflow();
        assertLogTagUnderflow();
        assertLogTagOverflow();
    }

    private static void assertAllLevelOK() {
        assertAllLevelsForTag(JFR_LOG_TAG, "");
        assertAllTagsForLevel(DEFAULT_TEST_LOG_LEVEL, "");
    }

    private static void assertFormatSpecifierOK() {
        assertAllTagsForLevel(DEFAULT_TEST_LOG_LEVEL, "%s");
        assertAllTagsForLevel(DEFAULT_TEST_LOG_LEVEL, "%n");
        assertAllTagsForLevel(DEFAULT_TEST_LOG_LEVEL, "%h");
    }

    private static void assertLargeMessageOK() {
        StringBuilder large = new StringBuilder();
        for (int i = 0; i < 1000 * 1000; i++) {
            large.append('o');
        }
        Logger.log(JFR_LOG_TAG, DEFAULT_TEST_LOG_LEVEL, large.toString());
    }

    private static void assertAllCharsOK() {
        char c = Character.MIN_VALUE;
        StringBuilder large = new StringBuilder();
        int logSize = 0;
        for(int i = Character.MIN_VALUE; i< (int)Character.MAX_VALUE; i++, c++) {
            large.append(c);
            logSize++;
            if (logSize == 80) {
                Logger.log(JFR_LOG_TAG, DEFAULT_TEST_LOG_LEVEL, large.toString());
                large = new StringBuilder();
            }
        }
    }

    private static void assertEmptyMessageOK() {
        assertAllLevelsForTag(JFR_LOG_TAG, "");
    }

    private static void assertNullMessageOK() {
        assertAllLevelsForTag(JFR_LOG_TAG, (String)null);
    }

    private static void assertLogLevelUnderflow() {
        try {
            JVM.log(JFR_LOG_TAG.ordinal(), 0, (String)null);
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }

    private static void assertLogLevelOverflow() {
        try {
            JVM.log(JFR_LOG_TAG.ordinal(), LogLevel.ERROR.ordinal() + 1, (String)null);
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }

    private static void assertLogTagUnderflow() {
        try {
            JVM.log(JFR_LOG_TAG.ordinal() - 1, DEFAULT_TEST_LOG_LEVEL.ordinal(), (String)null);
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }

    // since the enum will grow with new entries, provoking an overflow is problematic
    // is there a built-in "current max" in a Java enum
    private static void assertLogTagOverflow() {
        try {
            JVM.log(10000, DEFAULT_TEST_LOG_LEVEL.ordinal(), (String)null);
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }
}
