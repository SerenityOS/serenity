/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8216363
 * @summary Test that Handler.isLoggable(null) returns false
 * @run main/othervm IsLoggableHandlerTest
 */
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.MemoryHandler;
import java.util.logging.StreamHandler;
import java.util.stream.Stream;

public class IsLoggableHandlerTest {


    public static void main(String... args) throws IOException {
        String userDir = System.getProperty("user.dir", ".");
        File logfile = new File(userDir, "IsLoggableHandlerTest_" + UUID.randomUUID() + ".log");
        try {
            System.out.println("Dummy logfile: " + logfile.getAbsolutePath());
            Handler h = new CustomHandler();
            testIsLoggable(h);
            testIsLoggable(new MemoryHandler(h, 1, Level.ALL));
            testIsLoggable(new StreamHandler(System.out, new java.util.logging.SimpleFormatter()));
            testIsLoggable(new FileHandler(logfile.getAbsolutePath()));
            testIsLoggable(new ConsoleHandler());
        } finally {
            if (logfile.canRead()) {
                try {
                    System.out.println("Deleting dummy logfile: " + logfile.getAbsolutePath());
                    logfile.delete();
                } catch (Throwable t) {
                    System.out.println("Warning: failed to delete dummy logfile: " + t);
                    t.printStackTrace();
                }
            }
        }
    }

    public static void testIsLoggable(Handler h) {
        System.out.println("Testing " + h.getClass().getName());
        // should not throw NPE but return false
        if (h.isLoggable(null)) {
            throw new AssertionError(h.getClass().getName()
                    + ": null record should not be loggable");
        }
        h.setLevel(Level.ALL);
        // should still not throw NPE but return false
        if (h.isLoggable(null)) {
            throw new AssertionError(h.getClass().getName()
                    + ": null record should not be loggable");
        }
        // should not throw NPE
        h.publish(null);
    }

    public static final class CustomHandler extends Handler {
        @Override
        public void publish(LogRecord record) { }
        @Override
        public void flush() { }
        @Override
        public void close() throws SecurityException { }
    }


}
