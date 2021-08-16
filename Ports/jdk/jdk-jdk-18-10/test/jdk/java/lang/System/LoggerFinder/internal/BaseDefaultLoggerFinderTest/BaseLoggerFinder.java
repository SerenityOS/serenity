/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.logger.DefaultLoggerFinder;
import jdk.internal.logger.SimpleConsoleLogger;
import sun.util.logging.PlatformLogger;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class BaseLoggerFinder extends DefaultLoggerFinder
        implements BaseDefaultLoggerFinderTest.TestLoggerFinder {

    public BaseLoggerFinder() {
        if (fails.get()) {
            throw new RuntimeException("Simulate exception while loading provider");
        }
    }

    @Override
    public void setLevel(Logger logger, Level level, Module caller) {
        PrivilegedAction<Void> pa = () -> {
            setLevel(logger, PlatformLogger.toPlatformLevel(level), caller);
            return null;
        };
        AccessController.doPrivileged(pa);
    }

    @Override
    public void setLevel(Logger logger, PlatformLogger.Level level, Module caller) {
        PrivilegedAction<Logger> pa = () -> demandLoggerFor(logger.getName(), caller);
        Logger impl = AccessController.doPrivileged(pa);
        SimpleConsoleLogger.class.cast(impl)
                .getLoggerConfiguration()
                .setPlatformLevel(level);
    }

    @Override
    public PlatformLogger.Bridge asPlatformLoggerBridge(Logger logger) {
        PrivilegedAction<PlatformLogger.Bridge> pa = () ->
                PlatformLogger.Bridge.convert(logger);
        return AccessController.doPrivileged(pa);
    }

}

