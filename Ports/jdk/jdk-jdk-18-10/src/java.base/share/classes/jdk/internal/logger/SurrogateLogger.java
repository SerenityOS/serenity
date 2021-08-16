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

package jdk.internal.logger;

import java.util.function.Function;
import sun.util.logging.PlatformLogger;

/**
 * A simple console logger used to emulate the behavior of JUL loggers when
 * java.util.logging has no custom configuration.
 * Surrogate loggers are usually only used temporarily, until the LogManager
 * is initialized. At this point, the surrogates are replaced by an actual
 * logger obtained from LogManager.
 */
public final class SurrogateLogger extends SimpleConsoleLogger {

    private static final PlatformLogger.Level JUL_DEFAULT_LEVEL =
            PlatformLogger.Level.INFO;
    private static volatile String simpleFormatString;

    SurrogateLogger(String name) {
        super(name, true);
    }

    @Override
    PlatformLogger.Level defaultPlatformLevel() {
        return JUL_DEFAULT_LEVEL;
    }

    @Override
    String getSimpleFormatString() {
        if (simpleFormatString == null) {
            simpleFormatString = getSimpleFormat(null);
        }
        return simpleFormatString;
    }

    public static String getSimpleFormat(Function<String, String> defaultPropertyGetter) {
        return Formatting.getSimpleFormat(Formatting.JUL_FORMAT_PROP_KEY, defaultPropertyGetter);
    }

    public static SurrogateLogger makeSurrogateLogger(String name) {
        return new SurrogateLogger(name);
    }

    public static boolean isFilteredFrame(StackWalker.StackFrame st) {
        return Formatting.isFilteredFrame(st);
    }
}
