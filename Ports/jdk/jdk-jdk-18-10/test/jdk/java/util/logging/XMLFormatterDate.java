/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.XMLFormatter;

/**
 * @test
 * @bug 8028185
 * @summary XMLFormatter.format emits incorrect year (year + 1900)
 * @author dfuchs
 * @run main/othervm XMLFormatterDate
 */
public class XMLFormatterDate {

    static final class TimeStamp {

        final ZonedDateTime zdt;
        TimeStamp(ZoneId zoneId) {
            zdt = ZonedDateTime.now(zoneId);
        }
        int getYear() {
            return zdt.getYear();
        }
        boolean isJanuaryFirst() {
            return zdt.getMonth() == Month.JANUARY && zdt.getDayOfMonth() == 1;
        }
    }


    /**
     * Before the fix, JDK8 prints: {@code
     * <record>
     *   <date>3913-11-18T17:35:40</date>
     *   <millis>1384792540403</millis>
     *   <sequence>0</sequence>
     *   <level>INFO</level>
     *   <thread>1</thread>
     *   <message>test</message>
     * </record>
     * }
     * After the fix, it should print: {@code
     * <record>
     *   <date>2013-11-18T17:35:40</date>
     *   <millis>1384792696519</millis>
     *   <sequence>0</sequence>
     *   <level>INFO</level>
     *   <thread>1</thread>
     *   <message>test</message>
     * </record>
     * }
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Locale locale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);

            // Test with default format: by default date is in UTC.
            System.out.println("Testing with UTC");
            test(() -> new TimeStamp(ZoneOffset.UTC));

            // Change LogManager configuration so that new
            // XMLFormatter prints date in the pre Java 9 local zone format
            try {
                Properties props = new Properties();
                props.setProperty("java.util.logging.XMLFormatter.useInstant", "false");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                props.store(baos, "");
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                LogManager.getLogManager().updateConfiguration(bais, (k) -> (o,n) -> n!=null?n:o);
            } catch (IOException io) {
                throw new RuntimeException(io);
            }

            // re test with the old format: date will be in the local time zone.
            System.out.println("Testing with old format");
            test(() -> new TimeStamp(ZoneId.systemDefault()));

        } finally {
            Locale.setDefault(locale);
        }
    }

    static void test(Supplier<TimeStamp> timeStampSupplier) {

        TimeStamp t1 = timeStampSupplier.get();
        int year1 = t1.getYear();

        LogRecord record = new LogRecord(Level.INFO, "test");
        XMLFormatter formatter = new XMLFormatter();
        final String formatted = formatter.format(record);
        System.out.println(formatted);

        final TimeStamp t2 = timeStampSupplier.get();
        final int year2 = t2.getYear();
        if (year2 < 1900) {
            throw new Error("Invalid system year: " + year2);
        }

        final StringBuilder buf2 = new StringBuilder()
                .append("<date>").append(year2).append("-");
        if (!formatted.contains(buf2.toString())) {
            StringBuilder buf1 = new StringBuilder()
                    .append("<date>").append(year1).append("-");
            if (formatted.contains(buf1) && year2 == year1 + 1
                    && t2.isJanuaryFirst()) {
                // Oh! The year just switched in the midst of the test...
                System.out.println("Happy new year!");
            } else {
                throw new Error("Expected year " + year2
                        + " not found in log:\n" + formatted);
            }
        }
    }

}
