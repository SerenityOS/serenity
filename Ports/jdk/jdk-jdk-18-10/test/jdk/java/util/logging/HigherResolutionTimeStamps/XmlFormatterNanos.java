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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.XMLFormatter;

/**
 * @test
 * @bug 8072645
 * @summary tests that XmlFormatter will print out dates with the new
 *                nanosecond precision.
 * @run main/othervm XmlFormatterNanos
 * @author danielfuchs
 */
public class XmlFormatterNanos {

    static final int MILLIS_IN_SECOND = 1000;
    static final int NANOS_IN_MILLI = 1000_000;
    static final int NANOS_IN_MICRO = 1000;
    static final int NANOS_IN_SECOND = 1000_000_000;

    static final boolean verbose = true;

    static final class TestAssertException extends RuntimeException {
        TestAssertException(String msg) { super(msg); }
    }

    private static void assertEquals(long expected, long received, String msg) {
        if (expected != received) {
            throw new TestAssertException("Unexpected result for " + msg
                    + ".\n\texpected: " + expected
                    +  "\n\tactual:   " + received);
        } else if (verbose) {
            System.out.println("Got expected " + msg + ": " + received);
        }
    }

    private static void assertEquals(Object expected, Object received, String msg) {
        if (!Objects.equals(expected, received)) {
            throw new TestAssertException("Unexpected result for " + msg
                    + ".\n\texpected: " + expected
                    +  "\n\tactual:   " + received);
        } else if (verbose) {
            System.out.println("Got expected " + msg + ": " + received);
        }
    }

    static class CustomXMLFormatter extends XMLFormatter {}

    static class Configuration {
        final Properties propertyFile;
        private Configuration(Properties properties) {
            propertyFile = properties;
        }
        public Configuration apply() {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                propertyFile.store(bytes, testName());
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes.toByteArray());
                LogManager.getLogManager().readConfiguration(bais);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return this;
        }

        public static String useInstantProperty(Class<?> type) {
            return type.getName()+".useInstant";
        }

        public boolean useInstant(XMLFormatter formatter) {
            return Boolean.parseBoolean(propertyFile.getProperty(
                    formatter.getClass().getName()+".useInstant", "true"));
        }

        public String testName() {
            return propertyFile.getProperty("test.name", "????");
        }

        public static Configuration of(Properties props) {
            return new Configuration(props);
        }

        public static Configuration apply(Properties props) {
            return of(props).apply();
        }
    }

    static final List<Properties> properties;
    static {
        Properties props1 = new Properties();
        props1.setProperty("test.name", "with XML nano element (default)");
        Properties props2 = new Properties();
        props2.setProperty("test.name", "with XML nano element (standard=true, custom=false)");
        props2.setProperty(Configuration.useInstantProperty(XMLFormatter.class), "true");
        props2.setProperty(Configuration.useInstantProperty(CustomXMLFormatter.class), "false");
        Properties props3 = new Properties();
        props3.setProperty("test.name", "with XML nano element (standard=false, custom=true)");
        props3.setProperty(Configuration.useInstantProperty(XMLFormatter.class), "false");
        props3.setProperty(Configuration.useInstantProperty(CustomXMLFormatter.class), "true");

        properties = Collections.unmodifiableList(Arrays.asList(
                    props1,
                    props2));
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        properties.stream().forEach(XmlFormatterNanos::test);
    }

    static int getNanoAdjustment(LogRecord record) {
        return record.getInstant().getNano() % NANOS_IN_MILLI;
    }
    static void setNanoAdjustment(LogRecord record, int nanos) {
        record.setInstant(Instant.ofEpochSecond(record.getInstant().getEpochSecond(),
                (record.getInstant().getNano() / NANOS_IN_MILLI) * NANOS_IN_MILLI + nanos));
    }

    public static void test(Properties props) {
        Configuration conf = Configuration.apply(props);

        LogRecord record = new LogRecord(Level.INFO, "Test Name: {0}");
        record.setLoggerName("test");
        record.setParameters(new Object[] {conf.testName()});
        int nanos = record.getInstant().getNano() % NANOS_IN_MILLI;
        long millis = record.getMillis();
        // make sure we don't have leading zeros when printing below
        // the second precision
        if (millis % MILLIS_IN_SECOND < 100) millis = millis + 100;
        // make sure we some nanos - and make sure we don't have
        // trailing zeros
        if (nanos % 10 == 0) nanos = nanos + 7;

        record.setMillis(millis);
        setNanoAdjustment(record, nanos);
        final Instant instant = record.getInstant();
        if (nanos < 0) {
            throw new RuntimeException("Unexpected negative nano adjustment: "
                    + getNanoAdjustment(record));
        }
        if (nanos >= NANOS_IN_MILLI) {
            throw new RuntimeException("Nano adjustment exceeds 1ms: "
                    + getNanoAdjustment(record));
        }
        if (millis != record.getMillis()) {
            throw new RuntimeException("Unexpected millis: " + millis + " != "
                    + record.getMillis());
        }
        if (millis != record.getInstant().toEpochMilli()) {
            throw new RuntimeException("Unexpected millis: "
                    + record.getInstant().toEpochMilli());
        }
        long expectedNanos = (millis % MILLIS_IN_SECOND) * NANOS_IN_MILLI + nanos;
        assertEquals(expectedNanos, instant.getNano(), "Instant.getNano()");

        XMLFormatter formatter = new XMLFormatter();
        testMatching(formatter, record, instant, expectedNanos, conf.useInstant(formatter));

        XMLFormatter custom = new CustomXMLFormatter();
        testMatching(custom, record, instant, expectedNanos, conf.useInstant(custom));
    }

    private static void testMatching(XMLFormatter formatter,
            LogRecord record,  Instant instant, long expectedNanos,
            boolean useInstant) {

        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        int zdtNanos = zdt.getNano();
        assertEquals(expectedNanos, zdtNanos, "ZonedDateTime.getNano()");

        String str = formatter.format(record);

        String match = "."+expectedNanos;
        if (str.contains(match) != useInstant) {
            throw new RuntimeException(formatter.getClass().getSimpleName()
                    + ".format()"
                    + " string does not contain expected nanos: "
                    + "\n\texpected match for: '" + match + "'"
                    + "\n\tin: \n" + str);
        }
        System.out.println("Found expected match for '"+match+"' in \n"+str);

        match = "<millis>"+instant.toEpochMilli()+"</millis>";
        if (!str.contains(match)) {
            throw new RuntimeException(formatter.getClass().getSimpleName()
                    + ".format()"
                    + " string does not contain expected millis: "
                    + "\n\texpected match for: '" + match + "'"
                    + "\n\tin: \n" + str);
        }
        System.out.println("Found expected match for '"+match+"' in \n"+str);

        match = "<nanos>";
        if (str.contains(match) != useInstant) {
            throw new RuntimeException(formatter.getClass().getSimpleName()
                    + ".format()"
                    + " string "
                    + (useInstant
                            ? "does not contain expected nanos: "
                            : "contains unexpected nanos: ")
                    + "\n\t" + (useInstant ? "expected" : "unexpected")
                    + " match for: '" + match + "'"
                    + "\n\tin: \n" + str);
        }
        match = "<nanos>"+getNanoAdjustment(record)+"</nanos>";
        if (str.contains(match) != useInstant) {
            throw new RuntimeException(formatter.getClass().getSimpleName()
                    + ".format()"
                    + " string "
                    + (useInstant
                            ? "does not contain expected nanos: "
                            : "contains unexpected nanos: ")
                    + "\n\t" + (useInstant ? "expected" : "unexpected")
                    + " match for: '" + match + "'"
                    + "\n\tin: \n" + str);
        }
        if (useInstant) {
            System.out.println("Found expected match for '"+match+"' in \n"+str);
        } else {
            System.out.println("As expected '"+match+"' is not present in \n"+str);
        }

        match = useInstant ? DateTimeFormatter.ISO_INSTANT.format(instant)
                : zdt.truncatedTo(ChronoUnit.SECONDS)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        match = "<date>"+match+"</date>";
        if (!str.contains(match)) {
            throw new RuntimeException(formatter.getClass().getSimpleName()
                    + ".format()"
                    + " string does not contain expected date: "
                    + "\n\texpected match for: '" + match + "'"
                    + "\n\tin: \n" + str);
        }
        System.out.println("Found expected match for '"+match+"' in \n"+str);

    }

}
