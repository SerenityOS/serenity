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

package nsk.share.log;

import java.io.PrintStream;

/**
 * Basic Log that outputs to PrintStream.
 *
 * This log also tries to catch OutOfMemoryErrors and retry.
 *
 * @see nsk.share.log.Log
 */
public class LogSupport implements Log {
        private final int attempts = 2;
        private PrintStream out;
        private boolean infoEnabled = true;
        private boolean debugEnabled = true;
        private boolean warnEnabled = true;
        private boolean errorEnabled = true;

        public LogSupport() {
                this(System.out);
        }

        public LogSupport(PrintStream out) {
                this.out = out;
        }

        protected void logObject(Object o) {
                if (o instanceof Throwable) {
                        logThrowable((Throwable) o);
                        return;
                }
                for (int i = 0; i < attempts; ++i) {
                        try {
                                out.println(o.toString());
                                out.flush();
                                break;
                        } catch (OutOfMemoryError e) {
                                System.gc();
                                try {
                                        Thread.sleep(500);
                                } catch (InterruptedException ie) {
                                }
                        }
                }
                out.flush();
        }

        protected void logThrowable(Throwable o) {
                for (int i = 0; i < attempts; ++i) {
                        try {
                                o.printStackTrace(out);
                                out.flush();
                                break;
                        } catch (OutOfMemoryError e) {
                                System.gc();
                                try {
                                        Thread.sleep(500);
                                } catch (InterruptedException ie) {
                                }
                        }
                }
                out.flush();
        }

        public void info(Object o) {
                if (infoEnabled)
                        logObject(o);
        }

        public void debug(Object o) {
                if (debugEnabled)
                        logObject(o);
        }

        public void warn(Object o) {
                if (warnEnabled)
                        logObject(o);
        }

        public void error(Object o) {
                if (errorEnabled)
                        logObject(o);
        }

        public boolean isInfoEnabled() {
                return infoEnabled;
        }

        public void setInfoEnabled(boolean infoEnabled) {
                this.infoEnabled = infoEnabled;
        }

        public boolean isDebugEnabled() {
                return debugEnabled;
        }

        public void setDebugEnabled(boolean debugEnabled) {
                this.debugEnabled = debugEnabled;
        }

        public boolean isWarnEnabled() {
                return warnEnabled;
        }

        public void setWarnEnabled(boolean warnEnabled) {
                this.warnEnabled = warnEnabled;
        }

        public boolean isErrorEnabled() {
                return errorEnabled;
        }

        public void setErrorEnabled(boolean errorEnabled) {
                this.errorEnabled = errorEnabled;
        }

        public void setOut(PrintStream out) {
                this.out = out;
        }
}
