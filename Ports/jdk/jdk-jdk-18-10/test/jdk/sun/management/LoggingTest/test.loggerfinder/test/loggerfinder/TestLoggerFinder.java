/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package test.loggerfinder;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.System.LoggerFinder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.lang.StackWalker.StackFrame;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A LoggerFinder that provides System.Logger which print directly
 * on System.err, without involving java.logging.
 * For the purpose of the test, loggers whose name start with java.management.
 * will log all messages, and other loggers will only log level > INFO.
 * @author danielfuchs
 */
public class TestLoggerFinder extends LoggerFinder {

    static class TestLogger implements Logger {

        final String name;

        public TestLogger(String name) {
            this.name = name;
        }


        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isLoggable(Level level) {
            return name.equals("javax.management")
                    || name.startsWith("javax.management.")
                    || level.getSeverity() >= Level.INFO.getSeverity();
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
            if (!isLoggable(level)) return;
            publish(level, bundle, msg, thrown);
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            if (!isLoggable(level)) return;
            publish(level, bundle, format, params);
        }

        static void publish(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
            StackFrame sf = new CallerFinder().get().get();

            if (bundle != null && msg != null) {
                msg = bundle.getString(msg);
            }
            if (msg == null) msg = "";
            LocalDateTime ldt = LocalDateTime.now();
            String date = DateTimeFormatter.ISO_DATE_TIME.format(ldt);
            System.err.println(date + " "
                    + sf.getClassName() + " " + sf.getMethodName() + "\n"
                    + String.valueOf(level) + ": " + msg);
            thrown.printStackTrace(System.err);
        }

        static void publish(Level level, ResourceBundle bundle, String format, Object... params) {
            StackFrame sf = new CallerFinder().get().get();
            if (bundle != null && format != null) {
                format = bundle.getString(format);
            }
            String msg = format(format, params);
            LocalDateTime ldt = LocalDateTime.now();
            String date = DateTimeFormatter.ISO_DATE_TIME.format(ldt);
            System.err.println(date + " "
                    + sf.getClassName() + " " + sf.getMethodName() + "\n"
                    + String.valueOf(level) + ": " + msg);
        }

        static String format(String format, Object... args) {
            if (format == null) return "";
            int index = 0, len = format.length();
            while ((index = format.indexOf(index, '{')) >= 0) {
                if (index >= len - 2) break;
                char c = format.charAt(index+1);
                if (c >= '0' && c <= '9') {
                    return MessageFormat.format(format, args);
                }
                index++;
            }
            return format;
        }

    }

     /*
     * CallerFinder is a stateful predicate.
     */
    static final class CallerFinder implements Predicate<StackWalker.StackFrame> {
        private static final StackWalker WALKER;
        static {
            PrivilegedAction<StackWalker> pa =
                () -> StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            WALKER = AccessController.doPrivileged(pa);
        }

        /**
         * Returns StackFrame of the caller's frame.
         * @return StackFrame of the caller's frame.
         */
        Optional<StackWalker.StackFrame> get() {
            return WALKER.walk((s) -> s.filter(this).findFirst());
        }

        private boolean lookingForLogger = true;
        /**
         * Returns true if we have found the caller's frame, false if the frame
         * must be skipped.
         *
         * @param t The frame info.
         * @return true if we have found the caller's frame, false if the frame
         * must be skipped.
         */
        @Override
        public boolean test(StackWalker.StackFrame s) {
            // We should skip all frames until we have found the logger,
            // because these frames could be frames introduced by e.g. custom
            // sub classes of Handler.
            Class<?> c = s.getDeclaringClass();
            boolean isLogger = System.Logger.class.isAssignableFrom(c);
            if (lookingForLogger) {
                // Skip all frames until we have found the first logger frame.
                lookingForLogger = c != TestLogger.class;
                return false;
            }
            // Continue walking until we've found the relevant calling frame.
            // Skips logging/logger infrastructure.
            return !isLogger;
        }
    }

    @Override
    public Logger getLogger(String name, Module module) {
        return new TestLogger(name);
    }

}
