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

import java.io.*;
import java.util.*;

import java.util.logging.*;
import sun.util.logging.PlatformLogger;

/*
 * @test
 * @bug 8005615 8006104
 * @summary Add loggers to custom log manager
 *
 * @modules java.base/sun.util.logging
 *          java.logging
 * @compile -XDignore.symbol.file CustomLogManagerTest.java CustomLogManager.java
 * @run main/othervm -Djava.util.logging.manager=CustomLogManager CustomLogManagerTest
 */
public class CustomLogManagerTest {
    private static final String RESOURCE_BUNDLE = "sun.util.logging.resources.logging";
    public static void main(String[] args) {
        String mgr = System.getProperty("java.util.logging.manager");
        if (!mgr.equals("CustomLogManager")) {
            throw new RuntimeException("java.util.logging.manager not set");
        }

        Logger.getLogger(CustomLogManagerTest.class.getName());
        Logger.getLogger("org.foo.Foo");
        Logger.getLogger("org.foo.bar.Foo", RESOURCE_BUNDLE);
        // platform logger will be set with the default system resource bundle
        PlatformLogger.getLogger("org.openjdk.core.logger");

        if (LogManager.getLogManager() != CustomLogManager.INSTANCE) {
             throw new RuntimeException(LogManager.getLogManager() + " not CustomLogManager");
        }

        CustomLogManager.checkLogger(CustomLogManagerTest.class.getName());
        CustomLogManager.checkLogger("org.foo.Foo");
        CustomLogManager.checkLogger("org.foo.bar.Foo", RESOURCE_BUNDLE);
        CustomLogManager.checkLogger(Logger.GLOBAL_LOGGER_NAME);
        CustomLogManager.checkLogger("");
        CustomLogManager.checkLogger("org.openjdk.core.logger", RESOURCE_BUNDLE);
    }
}
