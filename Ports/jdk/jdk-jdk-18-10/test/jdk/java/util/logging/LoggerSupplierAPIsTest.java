/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005263
 * @run testng LoggerSupplierAPIsTest
 */

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test(groups="unit")
public class LoggerSupplierAPIsTest {
    static class CountingSupplier implements Supplier<String> {
        AtomicInteger sno = new AtomicInteger();

        @Override
        public String get() {
            return "Log message " + sno.getAndIncrement();
        }

        public int getCount() { return sno.get(); }
        public void reset() { sno.set(0); }
    }

    static class CountingHandler extends Handler {
        AtomicInteger count = new AtomicInteger();
        ArrayList<LogRecord> ar = new ArrayList<>();

        @Override
        public void close() { reset(); }

        @Override
        public void flush() { reset(); }

        @Override
        public void publish(LogRecord lr) {
            // Can do some more assertion here?
            // System.out.println(lr.getMessage());
            count.incrementAndGet();
        }

        public int getCount() {
            return count.get();
        }

        public List<LogRecord> getLogs() {
            return Collections.unmodifiableList(ar);
        }

        public void reset() {
            count.set(0);
            ar.clear();
        }
    }

    static class HelperEx extends Exception {
        final Level level;
        HelperEx(Level l) { level = l; }
        Level getLevel() { return level; }
    }

    static class SystemInfo {
        public static String Java() {
            return "Java: " + System.getProperty("java.version") +
                   " installed at " + System.getProperty("java.home");
        }

        public static String OS() {
            return "OS: " + System.getProperty("os.name") +
                   " " + System.getProperty("os.version") +
                   " " + System.getProperty("os.arch");
        }
    }

    static final CountingSupplier supplier = new CountingSupplier();
    static final CountingHandler handler = new CountingHandler();
    static final Logger logger;
    static final Level[] levels = { Level.ALL, Level.OFF, Level.SEVERE,
                                    Level.WARNING, Level.INFO, Level.CONFIG,
                                    Level.FINE, Level.FINER, Level.FINEST };
    static final int[] invokes = { 7, 0, 1, 2, 3, 4, 5, 6, 7 };
    static final int[] log_count = { 10, 0, 1, 2, 3, 4, 6, 8, 10 };

    static {
        logger = Logger.getLogger("LoggerSupplierApisTest");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
    }

    public void setup() {
        supplier.reset();
        handler.reset();
    }

    private void testLog() {
        logger.log(Level.SEVERE, supplier);
        logger.log(Level.WARNING, supplier);
        logger.log(Level.INFO, supplier);
        logger.log(Level.CONFIG, supplier);
        logger.log(Level.FINE, supplier);
        logger.log(Level.FINER, supplier);
        logger.log(Level.FINEST, supplier);
        // Lambda expression
        logger.log(Level.FINE, () ->
            "Timestamp: " + System.currentTimeMillis() +
            ", user home directory:  " + System.getProperty("user.home"));
        // Method reference
        logger.log(Level.FINER, SystemInfo::Java);
        logger.log(Level.FINEST, SystemInfo::OS);
    }

    private void testLogThrown() {
        logger.log(Level.SEVERE, new HelperEx(Level.SEVERE), supplier);
        logger.log(Level.WARNING, new HelperEx(Level.WARNING), supplier);
        logger.log(Level.INFO, new HelperEx(Level.INFO), supplier);
        logger.log(Level.CONFIG, new HelperEx(Level.CONFIG), supplier);
        logger.log(Level.FINE, new HelperEx(Level.FINE), supplier);
        logger.log(Level.FINER, new HelperEx(Level.FINER), supplier);
        logger.log(Level.FINEST, new HelperEx(Level.FINEST), supplier);
        // Lambda expression
        logger.log(Level.FINE, new HelperEx(Level.FINE), () ->
            "Timestamp: " + System.currentTimeMillis() +
            ", user home directory:  " + System.getProperty("user.home"));
        // Method reference
        logger.log(Level.FINER, new HelperEx(Level.FINER), SystemInfo::Java);
        logger.log(Level.FINEST, new HelperEx(Level.FINEST), SystemInfo::OS);
    }

    private void testLogp() {
        final String cls = getClass().getName();
        final String mtd = "testLogp";

        logger.logp(Level.SEVERE, cls, mtd, supplier);
        logger.logp(Level.WARNING, cls, mtd, supplier);
        logger.logp(Level.INFO, cls, mtd, supplier);
        logger.logp(Level.CONFIG, cls, mtd, supplier);
        logger.logp(Level.FINE, cls, mtd, supplier);
        logger.logp(Level.FINER, cls, mtd, supplier);
        logger.logp(Level.FINEST, cls, mtd, supplier);
        // Lambda expression
        logger.logp(Level.FINE, cls, mtd, () ->
            "Timestamp: " + System.currentTimeMillis() +
            ", user home directory:  " + System.getProperty("user.home"));
        // Method reference
        logger.logp(Level.FINER, cls, mtd, SystemInfo::Java);
        logger.logp(Level.FINEST, cls, mtd, SystemInfo::OS);
    }

    private void testLogpThrown() {
        final String cls = getClass().getName();
        final String mtd = "testLogpThrown";

        logger.logp(Level.SEVERE, cls, mtd, new HelperEx(Level.SEVERE), supplier);
        logger.logp(Level.WARNING, cls, mtd, new HelperEx(Level.WARNING), supplier);
        logger.logp(Level.INFO, cls, mtd, new HelperEx(Level.INFO), supplier);
        logger.logp(Level.CONFIG, cls, mtd, new HelperEx(Level.CONFIG), supplier);
        logger.logp(Level.FINE, cls, mtd, new HelperEx(Level.FINE), supplier);
        logger.logp(Level.FINER, cls, mtd, new HelperEx(Level.FINER), supplier);
        logger.logp(Level.FINEST, cls, mtd, new HelperEx(Level.FINEST), supplier);
        // Lambda expression
        logger.logp(Level.FINE, cls, mtd, new HelperEx(Level.FINE), () ->
            "Timestamp: " + System.currentTimeMillis() +
            ", user home directory:  " + System.getProperty("user.home"));
        // Method reference
        logger.logp(Level.FINER, cls, mtd, new HelperEx(Level.FINER), SystemInfo::Java);
        logger.logp(Level.FINEST, cls, mtd, new HelperEx(Level.FINEST), SystemInfo::OS);
    }

    private void testLevelConvenientMethods() {
        logger.severe(supplier);
        logger.warning(supplier);
        logger.info(supplier);
        logger.config(supplier);
        logger.fine(supplier);
        logger.finer(supplier);
        logger.finest(supplier);
        // Lambda expression
        logger.fine(() ->
            "Timestamp: " + System.currentTimeMillis() +
            ", user home directory:  " + System.getProperty("user.home"));
        // Method reference
        logger.finer(SystemInfo::Java);
        logger.finest(SystemInfo::OS);
    }

    private void validate(int index, boolean thrown, String methodName) {
        assertEquals(supplier.getCount(), invokes[index]);
        assertEquals(handler.getCount(), log_count[index]);
        // Verify associated Throwable is right
        if (thrown) {
            for (LogRecord r: handler.getLogs()) {
                assertTrue(r.getThrown() instanceof HelperEx, "Validate Thrown");
                HelperEx e = (HelperEx) r.getThrown();
                assertEquals(r.getLevel(), e.getLevel(), "Validate Thrown Log Level");
            }
        }

        if (methodName != null) {
            for (LogRecord r: handler.getLogs()) {
                assertEquals(r.getSourceClassName(), getClass().getName());
                assertEquals(r.getSourceMethodName(), methodName);
            }
        }
    }

    public void verifyLogLevel() {
        for (int i = 0; i < levels.length; i++) {
            logger.setLevel(levels[i]);

            setup();
            testLog();
            validate(i, false, null);

            setup();
            testLogThrown();
            validate(i, true, null);

            setup();
            testLogp();
            validate(i, false, "testLogp");

            setup();
            testLogpThrown();
            validate(i, true, "testLogpThrown");

            setup();
            testLevelConvenientMethods();
            validate(i, false, null);
        }
    }
}
