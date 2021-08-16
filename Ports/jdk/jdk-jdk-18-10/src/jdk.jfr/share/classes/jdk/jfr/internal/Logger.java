/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal;

import java.util.Arrays;

/**
 * JFR logger
 *
 */

public final class Logger {

    private static final int MAX_SIZE = 10_000;
    private static final int MAX_EVENT_SIZE = 100_000;
    static {
        // This will try to initialize the JVM logging system
        JVMSupport.tryToInitializeJVM();
    }


    public static void log(LogTag logTag, LogLevel logLevel, String message) {
        if (shouldLog(logTag, logLevel)) {
            logInternal(logTag, logLevel, message);
        }
    }

    public static void logEvent(LogLevel logLevel, String[] lines, boolean system) {
        if (lines == null || lines.length == 0) {
            return;
        }
        if (shouldLog(LogTag.JFR_EVENT, logLevel) || shouldLog(LogTag.JFR_SYSTEM_EVENT, logLevel)) {
            int size = 0;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (size + line.length() > MAX_EVENT_SIZE) {
                    lines = Arrays.copyOf(lines, i + 1);
                    lines[i] = "...";
                    break;
                }
                size+=line.length();
            }
            JVM.logEvent(logLevel.level, lines, system);
        }
    }

    private static void logInternal(LogTag logTag, LogLevel logLevel, String message) {
        if (message == null || message.length() < MAX_SIZE) {
            JVM.log(logTag.id, logLevel.level, message);
        } else {
            JVM.log(logTag.id, logLevel.level, message.substring(0, MAX_SIZE));
        }
    }

    public static boolean shouldLog(LogTag tag, LogLevel level) {
        return level.level >= tag.tagSetLevel;
    }
}
