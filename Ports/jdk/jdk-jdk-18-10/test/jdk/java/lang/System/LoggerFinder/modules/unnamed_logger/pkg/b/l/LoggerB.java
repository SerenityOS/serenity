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

package pkg.b.l;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.ResourceBundle;

public class LoggerB implements System.Logger {

    // ---- test utility fields and methods ----

    private static Map<String, LoggerB> map = new HashMap<>();

    public static LoggerB getLogger(String name) {
        return map.computeIfAbsent(name, (n) -> new LoggerB());
    }

    public static boolean checkLog(String name, Level level, ResourceBundle bundle,
                                   String format, Throwable throwable, Object... params) {
        LoggerB logger = map.get(name);
        LogEvent event = new LogEvent(level, bundle, format, null, params);
        for (LogEvent l : logger.queue) {
            if (l.equals(event)) {
                return true;
            }
        }
        return false;
    }

    // ---- logger implementation ----

    private Queue<LogEvent> queue = new LinkedList<>();

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isLoggable(Level level) {
        return true;
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) {
        String msg = bundle != null ? bundle.getString(format) : format;
        log(new LogEvent(level, bundle, msg, null, params));
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Throwable throwable) {
        String msg = bundle != null ? bundle.getString(format) : format;
        log(new LogEvent(level, bundle, msg, throwable, (Object)null));
    }

    void log(LogEvent l) {
        print(l);
        queue.add(l);
    }

    private void print(LogEvent l) {
        System.err.println("LoggerB Message"+ l);
    }

    public Queue<LogEvent> getLogEvent() {
        return queue;
    }

    public static class LogEvent {
        public LogEvent(Level level, ResourceBundle bundle, String format,
                        Throwable throwable, Object... params) {
            this.level = level;
            this.bundle = bundle;
            this.format = format;
            this.throwable = throwable;
            this.params = params;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof LogEvent) {
                LogEvent e = (LogEvent)o;
                return level == e.level
                    && bundle == e.bundle
                    && format == e.format
                    && params == e.params;
            }
            return false;
        }

        private Level level;
        private ResourceBundle bundle;
        private String format;
        private Throwable throwable;
        private Object[] params;
    }
}
