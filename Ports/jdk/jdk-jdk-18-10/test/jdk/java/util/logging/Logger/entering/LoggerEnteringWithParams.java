/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * @test
 * @bug 8028788
 * @summary tests that the message format string is correctly constructed
 *          by Logger.entering
 * @run main/othervm LoggerEnteringWithParams
 * @author daniel fuchs
 */
public class LoggerEnteringWithParams {

    static final Object[] data = {
        "one", "two", "three", "four", "five", "six", "seven", "eight"
    };

    static final class TestHandler extends Handler {
        final List<LogRecord> events = new CopyOnWriteArrayList<>();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final StreamHandler wrapped = new StreamHandler(bytes, new SimpleFormatter());

        @Override
        public void publish(LogRecord record) {
            events.add(record);
            wrapped.publish(record);
            wrapped.flush();
        }
        @Override
        public void flush() {
            wrapped.flush();
        }
        @Override
        public void close() throws SecurityException {
            wrapped.close();
        }

        @Override
        public synchronized void setLevel(Level newLevel) throws SecurityException {
            super.setLevel(newLevel);
            wrapped.setLevel(newLevel);
        }

        public void reset() {
            bytes.reset();
            events.clear();
        }

    }

    public static void main(String[] args) {
        Logger logger = Logger.getLogger("some.logger");
        TestHandler handler = new TestHandler();
        logger.setUseParentHandlers(false);
        handler.setLevel(Level.ALL);
        logger.setLevel(Level.FINEST);
        logger.addHandler(handler);

        // Auto-detect the line termination used by SimpleFormatter - (CR vs CRLF)
        logger.entering("test", "test");
        final String test = handler.bytes.toString();
        System.out.println(test);
        final String lineEnd = test.substring(test.indexOf("ENTRY")+"ENTRY".length());
        System.out.print("Line termination is " + lineEnd.length() + " char(s) long:");
        for (int i=0; i<lineEnd.length(); i++) {
            System.out.print(" code="+(int)lineEnd.charAt(i));
        }
        System.out.println();
        handler.reset();

        for (int i=0 ; i<=data.length; i++) {
            final StringBuilder b = new StringBuilder("ENTRY");
            final StringBuilder f = new StringBuilder("ENTRY");
            final Object[] params = new Object[i];
            for (int k=0; k<i; k++) {
                params[k] = data[k];
                b.append(' ').append(String.valueOf(params[k]));
                f.append(" {").append(String.valueOf(k)).append('}');
            }

            final String expected = b.toString();
            final String format = f.toString();
            final String className = "class"+i;
            final String methodName = "method"+i;

            logger.entering(className, methodName, params);
            final String logged = handler.bytes.toString();
            System.out.println("got:\n" + logged);

            if (!logged.contains(className)) {
                throw new RuntimeException("Marker for " + className
                        + " not found."
                        + "\n\tgot: <<\n" + logged + "\t     >>");
            }
            if (!logged.contains(methodName)) {
                throw new RuntimeException("Marker for " + methodName
                        + " not found."
                        + "\n\tgot: <<\n" + logged + "\t     >>");
            }
            if (!logged.contains(expected)) {
                throw new RuntimeException("Marker for parameters[size="
                        + i + "] not found"
                        + "\n\tgot: <<\n" + logged + "\t     >>");
            }
            if (!logged.contains(expected+lineEnd)) {
                throw new RuntimeException("Marker for parameters[size="
                        + i + "] has extra characters"
                        + "\n\tgot: <<\n" + logged + "\t     >>");
            }

            LogRecord record = handler.events.remove(0);
            if (!handler.events.isEmpty()) {
                throw new RuntimeException("Handler has more log records: "
                        + handler.events.toString());
            }
            if (!className.equals(record.getSourceClassName())) {
                throw new RuntimeException("Unexpected class name in LogRecord."
                        + "\n\texpected: " + className
                        + "\n\tgot: " + record.getSourceClassName());
            }
            if (!methodName.equals(record.getSourceMethodName())) {
                throw new RuntimeException("Unexpected method name in LogRecord."
                        + "\n\texpected: " + methodName
                        + "\n\tgot: " + record.getSourceMethodName());
            }
            if (!format.equals(record.getMessage())) {
                throw new RuntimeException("Unexpected message format in LogRecord."
                        + "\n\texpected: " + format
                        + "\n\tgot: " + record.getMessage());
            }
            if (!Arrays.deepEquals(params, record.getParameters())) {
                throw new RuntimeException("Unexpected parameter array in LogRecord."
                        + "\n\texpected: " + Arrays.toString(params)
                        + "\n\tgot: " + Arrays.toString(record.getParameters()));
            }

            handler.reset();
        }
    }

}
