/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.lang.System.Logger;

/**
 * What our test provider needs to implement.
 * @author danielfuchs
 */
public interface TestLoggerFinder {
    public final static AtomicLong sequencer = new AtomicLong();
    public final ConcurrentHashMap<String, LoggerImpl> system = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, LoggerImpl> user = new ConcurrentHashMap<>();
    public final Queue<LogEvent> eventQueue = new ArrayBlockingQueue<>(128);

    public static final class LogEvent {

        public LogEvent() {
            this(sequencer.getAndIncrement());
        }

        LogEvent(long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }

        long sequenceNumber;
        boolean isLoggable;
        String loggerName;
        Logger.Level level;
        ResourceBundle bundle;
        Throwable thrown;
        Object[] args;
        Supplier<String> supplier;
        String msg;

        Object[] toArray() {
            return new Object[] {
                sequenceNumber,
                isLoggable,
                loggerName,
                level,
                bundle,
                thrown,
                args,
                supplier,
                msg,
            };
        }

        @Override
        public String toString() {
            return Arrays.deepToString(toArray());
        }



        @Override
        public boolean equals(Object obj) {
            return obj instanceof LogEvent
                    && Objects.deepEquals(this.toArray(), ((LogEvent)obj).toArray());
        }

        @Override
        public int hashCode() {
            return Objects.hash(toArray());
        }


        public static LogEvent of(boolean isLoggable, String name,
                Logger.Level level, ResourceBundle bundle,
                String key, Throwable thrown) {
            LogEvent evt = new LogEvent();
            evt.isLoggable = isLoggable;
            evt.loggerName = name;
            evt.level = level;
            evt.args = null;
            evt.bundle = bundle;
            evt.thrown = thrown;
            evt.supplier = null;
            evt.msg = key;
            return evt;
        }

        public static LogEvent of(boolean isLoggable, String name,
                Logger.Level level, ResourceBundle bundle,
                String key, Object... params) {
            LogEvent evt = new LogEvent();
            evt.isLoggable = isLoggable;
            evt.loggerName = name;
            evt.level = level;
            evt.args = params;
            evt.bundle = bundle;
            evt.thrown = null;
            evt.supplier = null;
            evt.msg = key;
            return evt;
        }

        public static LogEvent of(long sequenceNumber,
                boolean isLoggable, String name,
                Logger.Level level, ResourceBundle bundle,
                String key, Supplier<String> supplier,
                Throwable thrown, Object... params) {
            LogEvent evt = new LogEvent(sequenceNumber);
            evt.loggerName = name;
            evt.level = level;
            evt.args = params;
            evt.bundle = bundle;
            evt.thrown = thrown;
            evt.supplier = supplier;
            evt.msg = key;
            evt.isLoggable = isLoggable;
            return evt;
        }

    }

    public class LoggerImpl implements Logger {
        final String name;
        Logger.Level level = Logger.Level.INFO;

        public LoggerImpl(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isLoggable(Logger.Level level) {
            return this.level != Logger.Level.OFF && this.level.getSeverity() <= level.getSeverity();
        }

        @Override
        public void log(Logger.Level level, ResourceBundle bundle, String key, Throwable thrown) {
            log(LogEvent.of(isLoggable(level), this.name, level, bundle, key, thrown));
        }

        @Override
        public void log(Logger.Level level, ResourceBundle bundle, String format, Object... params) {
            log(LogEvent.of(isLoggable(level), name, level, bundle, format, params));
        }

        void log(LogEvent event) {
            eventQueue.add(event);
        }
    }

    public Logger getLogger(String name, Module caller);
    public Logger getLocalizedLogger(String name, ResourceBundle bundle, Module caller);
}
