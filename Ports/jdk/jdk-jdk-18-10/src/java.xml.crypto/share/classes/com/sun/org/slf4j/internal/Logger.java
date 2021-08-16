/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.org.slf4j.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;

// Bridge to java.util.logging.
public class Logger {

    /**
     * StackWalker to find out the caller of this class so that it can be
     * shown in the log output. The multiple private log0() methods below
     * skip exactly 2 frames -- one log0() itself, the other one of the
     * public debug()/warn()/error()/trace() methods in this class --
     * to find the caller.
     */
    @SuppressWarnings("removal")
    private static final StackWalker WALKER = AccessController.doPrivileged(
            (PrivilegedAction<StackWalker>)
            () -> StackWalker.getInstance(
                    StackWalker.Option.RETAIN_CLASS_REFERENCE));

    private final java.util.logging.Logger impl;

    public Logger(String name) {
        impl = java.util.logging.Logger.getLogger(name);
    }

    public boolean isDebugEnabled() {
        return impl.isLoggable(Level.FINE);
    }

    public boolean isTraceEnabled() {
        return impl.isLoggable(Level.FINE);
    }

    public void debug(String s) {
        log0(Level.FINE, s);
    }

    public void debug(String s, Throwable e) {
        log0(Level.FINE, s, e);
    }

    public void debug(String s, Object... o) {
        log0(Level.FINE, s, o);
    }

    public void trace(String s) {
        log0(Level.FINE, s);
    }

    public void error(String s) {
        log0(Level.SEVERE, s);
    }

    public void error(String s, Throwable e) {
        log0(Level.SEVERE, s, e);
    }

    public void error(String s, Object... o) {
        log0(Level.SEVERE, s, o);
    }

    public void warn(String s) {
        log0(Level.WARNING, s);
    }

    public void warn(String s, Throwable e) {
        log0(Level.WARNING, s, e);
    }

    public void warn(String s, Object... o) {
        log0(Level.WARNING, s, o);
    }

    private void log0(Level level, String s) {
        if (impl.isLoggable(level)) {
            var sf = WALKER.walk(f -> f.skip(2).findFirst()).get();
            impl.logp(Level.FINE, sf.getClassName(), sf.getMethodName(), s);
        }
    }

    private void log0(Level level, String s, Throwable e) {
        if (impl.isLoggable(level)) {
            var sf = WALKER.walk(f -> f.skip(2).findFirst()).get();
            impl.logp(Level.FINE, sf.getClassName(), sf.getMethodName(), s, e);
        }
    }

    private void log0(Level level, String s, Object... o) {
        if (impl.isLoggable(level)) {
            var sf = WALKER.walk(f -> f.skip(2).findFirst()).get();
            impl.logp(Level.FINE, sf.getClassName(), sf.getMethodName(),
                    addIndex(s), o);
        }
    }

    /**
     * Translate the log4j message format "Hello {}, {}" to the
     * java.util.logging format "Hello {0}, {1}".
     */
    private static String addIndex(String s) {
        int start = 0;
        int index = 0;
        StringBuilder sb = new StringBuilder();
        while (true) {
            int pos = s.indexOf("{}", start);
            if (pos < 0) {
                break;
            }
            sb.append(s, start, pos + 1).append(index++);
            start = pos + 1;
        }
        if (index == 0) {
            return s;
        } else {
            sb.append(s, start, s.length());
            return sb.toString();
        }
    }
}
