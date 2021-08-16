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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * @test
 * @bug 8072645 8144262
 * @summary tests the new methods added to LogRecord.
 * @run main LogRecordWithNanosAPI
 * @author danielfuchs
 */
public class LogRecordWithNanosAPI {

    static final int MILLIS_IN_SECOND = 1000;
    static final int NANOS_IN_MILLI = 1000_000;
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

    // The nano second fractional part of a second, contained in a time expressed
    // as a number of millisecond from the epoch.
    private static long nanoInSecondFromEpochMilli(long millis) {
        return (((millis%MILLIS_IN_SECOND) + MILLIS_IN_SECOND)%MILLIS_IN_SECOND)*NANOS_IN_MILLI;
    }

    /**
     * Serializes a log record, then deserializes it and check that both
     * records match.
     * @param record the log record to serialize & deserialize.
     * @param hasExceedingNanos whether the record has a nano adjustment whose
     *            value exceeds 1ms.
     * @throws IOException Unexpected.
     * @throws ClassNotFoundException  Unexpected.
     */
    public static void test(LogRecord record, boolean hasExceedingNanos)
            throws IOException, ClassNotFoundException {

        // Format the given logRecord using the SimpleFormatter
        SimpleFormatter formatter = new SimpleFormatter();
        String str = formatter.format(record);

        // Serialize the given LogRecord
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(record);
        oos.flush();
        oos.close();

        // First checks that the log record can be deserialized
        final ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bais);
        final LogRecord record2 = (LogRecord)ois.readObject();

        assertEquals(record.getMillis(), record2.getMillis(), "getMillis()");
        assertEquals(record.getInstant().getEpochSecond(),
                record2.getInstant().getEpochSecond(),
                "getInstant().getEpochSecond()");
        assertEquals(record.getInstant().getNano(),
                record2.getInstant().getNano(),
                "getInstant().getNano()");
        assertEquals(record.getInstant().toEpochMilli(),
                record2.getInstant().toEpochMilli(),
                "getInstant().toEpochMilli()");
        long millis = record.getMillis();
        millis = hasExceedingNanos
                ? Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                        (millis%MILLIS_IN_SECOND)*NANOS_IN_MILLI
                        + record.getInstant().getNano() % NANOS_IN_MILLI).toEpochMilli()
                : millis;
        assertEquals(millis, record.getInstant().toEpochMilli(),
                "getMillis()/getInstant().toEpochMilli()");
        millis = record2.getMillis();
        millis = hasExceedingNanos
                ? Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                        (millis%MILLIS_IN_SECOND)*NANOS_IN_MILLI
                        + record2.getInstant().getNano() % NANOS_IN_MILLI).toEpochMilli()
                : millis;
        assertEquals(millis, record2.getInstant().toEpochMilli(),
                "getMillis()/getInstant().toEpochMilli()");
        long nanos = nanoInSecondFromEpochMilli(record.getMillis())
                + record.getInstant().getNano() % NANOS_IN_MILLI;
        assertEquals(nanos, record.getInstant().getNano(),
                "nanoInSecondFromEpochMilli(record.getMillis())"
                + " + record.getInstant().getNano() % NANOS_IN_MILLI /getInstant().getNano()");
        nanos = nanoInSecondFromEpochMilli(record2.getMillis())
                + record2.getInstant().getNano() % NANOS_IN_MILLI;
        assertEquals(nanos, record2.getInstant().getNano(),
                "nanoInSecondFromEpochMilli(record2.getMillis())"
                + " + record2.getInstant().getNano() % NANOS_IN_MILLI /getInstant().getNano()");

        // Format the deserialized LogRecord using the SimpleFormatter, and
        // check that the string representation obtained matches the string
        // representation of the original LogRecord
        String str2 = formatter.format(record2);
        if (!str.equals(str2))
            throw new RuntimeException("Unexpected values in deserialized object:"
                + "\n\tExpected:  " + str
                + "\n\tRetrieved: "+str);

    }


    public static void main(String[] args) throws Exception {
        int count=0;
        LogRecord record = new LogRecord(Level.INFO, "Java Version: {0}");
        record.setLoggerName("test");
        record.setParameters(new Object[] {System.getProperty("java.version")});
        final int nanos = record.getInstant().getNano() % NANOS_IN_MILLI;
        final long millis = record.getMillis();
        final Instant instant = record.getInstant();
        if (millis != instant.toEpochMilli()) {
            throw new RuntimeException("Unexpected millis: "
                    + record.getMillis());
        }
        test(record, false);

        // nano adjustment < 1ms (canonical case)
        int newNanos = (nanos + 111111) % NANOS_IN_MILLI;
        record.setInstant(Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                (millis % MILLIS_IN_SECOND) * NANOS_IN_MILLI + newNanos));
        assertEquals(newNanos, record.getInstant().getNano() % NANOS_IN_MILLI,
                "record.getInstant().getNano() % NANOS_IN_MILLI");
        assertEquals(millis, record.getMillis(), "record.getMillis()");
        assertEquals(Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                (millis%MILLIS_IN_SECOND)*NANOS_IN_MILLI + newNanos),
                record.getInstant(), "record.getInstant()");
        test(record, false);
        assertEquals(newNanos, record.getInstant().getNano() % NANOS_IN_MILLI,
                "record.getInstant().getNano() % NANOS_IN_MILLI");
        assertEquals(millis, record.getMillis(), "record.getMillis()");
        assertEquals(Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                (millis%MILLIS_IN_SECOND)*NANOS_IN_MILLI + newNanos),
                record.getInstant(), "record.getInstant()");

        // nano adjustment > 1ms - non canonical - should still work
        int newExceedingNanos = 2111_111;
        record.setInstant(Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                (millis % MILLIS_IN_SECOND) * NANOS_IN_MILLI + newExceedingNanos));
        assertEquals(newExceedingNanos % NANOS_IN_MILLI,
                record.getInstant().getNano() % NANOS_IN_MILLI,
                "record.getInstant().getNano() % NANOS_IN_MILLI");
        assertEquals(millis + newExceedingNanos / NANOS_IN_MILLI,
                record.getMillis(), "record.getMillis()");
        assertEquals(Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                (millis%MILLIS_IN_SECOND)*NANOS_IN_MILLI + newExceedingNanos),
                record.getInstant(), "record.getInstant()");
        test(record, true);
        assertEquals(newExceedingNanos % NANOS_IN_MILLI,
                record.getInstant().getNano() % NANOS_IN_MILLI,
                "record.getInstant().getNano() % NANOS_IN_MILLI");
        assertEquals(millis  + newExceedingNanos / NANOS_IN_MILLI,
                record.getMillis(), "record.getMillis()");
        assertEquals(Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                (millis%MILLIS_IN_SECOND)*NANOS_IN_MILLI + newExceedingNanos),
                record.getInstant(), "record.getInstant()");

        // nano adjustement > 1s - non canonical - should still work
        newExceedingNanos = 1111_111_111;
        record.setInstant(Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                (millis % MILLIS_IN_SECOND) * NANOS_IN_MILLI + newExceedingNanos));
        assertEquals(newExceedingNanos % NANOS_IN_MILLI,
                record.getInstant().getNano() % NANOS_IN_MILLI,
                "record.getInstant().getNano() % NANOS_IN_MILLI");
        assertEquals(millis  + newExceedingNanos / NANOS_IN_MILLI,
                record.getMillis(), "record.getMillis()");
        assertEquals(Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                (millis%MILLIS_IN_SECOND)*NANOS_IN_MILLI + newExceedingNanos),
                record.getInstant(), "record.getInstant()");
        test(record, true);
        assertEquals(newExceedingNanos % NANOS_IN_MILLI,
                record.getInstant().getNano() % NANOS_IN_MILLI,
                "record.getInstant().getNano() % NANOS_IN_MILLI");
        assertEquals(millis  + newExceedingNanos / NANOS_IN_MILLI,
                record.getMillis(), "record.getMillis()");
        assertEquals(Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                (millis%MILLIS_IN_SECOND)*NANOS_IN_MILLI + newExceedingNanos),
                record.getInstant(), "record.getInstant()");

        // nano adjustement < 0 - non canonical - should still work
        newExceedingNanos = -1;
        record.setInstant(Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                (millis % MILLIS_IN_SECOND) * NANOS_IN_MILLI + newExceedingNanos));
        assertEquals(newExceedingNanos + NANOS_IN_MILLI,
                record.getInstant().getNano() % NANOS_IN_MILLI,
                "record.getInstant().getNano() % NANOS_IN_MILLI");
        assertEquals(millis -1, record.getMillis(), "record.getMillis()");
        assertEquals(Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                (millis%MILLIS_IN_SECOND)*NANOS_IN_MILLI + newExceedingNanos),
                record.getInstant(), "record.getInstant()");
        test(record, true);
        record.setInstant(Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                (millis % MILLIS_IN_SECOND) * NANOS_IN_MILLI + newExceedingNanos));
        assertEquals(millis -1, record.getMillis(), "record.getMillis()");
        assertEquals(Instant.ofEpochSecond(millis/MILLIS_IN_SECOND,
                (millis%MILLIS_IN_SECOND)*NANOS_IN_MILLI + newExceedingNanos),
                record.getInstant(), "record.getInstant()");

        // setMillis
        record.setMillis(millis-1);
        assertEquals(millis-1, record.getInstant().toEpochMilli(),
                "record.getInstant().toEpochMilli()");
        assertEquals(millis-1, record.getMillis(),
                "record.getMillis()");
        assertEquals(0, record.getInstant().getNano() % NANOS_IN_MILLI,
                "record.getInstant().getNano() % NANOS_IN_MILLI");
        test(record, false);
        assertEquals(millis-1, record.getInstant().toEpochMilli(),
                "record.getInstant().toEpochMilli()");
        assertEquals(millis-1, record.getMillis(),
                "record.getMillis()");
        assertEquals(0, record.getInstant().getNano() % NANOS_IN_MILLI,
                "record.getInstant().getNano() % NANOS_IN_MILLI");

        // setMillis to 0
        record.setMillis(0);
        assertEquals(0, record.getInstant().toEpochMilli(),
                "record.getInstant().toEpochMilli()");
        assertEquals(0, record.getMillis(),
                "record.getMillis()");
        assertEquals(0, record.getInstant().getNano() % NANOS_IN_MILLI,
                "record.getInstant().getNano() % NANOS_IN_MILLI");
        test(record, false);
        assertEquals(0, record.getInstant().toEpochMilli(),
                "record.getInstant().toEpochMilli()");
        assertEquals(0, record.getMillis(),
                "record.getMillis()");
        assertEquals(0, record.getInstant().getNano() % NANOS_IN_MILLI,
                "record.getInstant().getNano() % NANOS_IN_MILLI");

        // setMillis to -1
        record.setMillis(-1);
        assertEquals(-1, record.getInstant().toEpochMilli(),
                "record.getInstant().toEpochMilli()");
        assertEquals(-1, record.getMillis(),
                "record.getMillis()");
        assertEquals(0, record.getInstant().getNano() % NANOS_IN_MILLI,
                "record.getInstant().getNano() % NANOS_IN_MILLI");
        test(record, false);
        assertEquals(-1, record.getInstant().toEpochMilli(),
                "record.getInstant().toEpochMilli()");
        assertEquals(-1, record.getMillis(),
                "record.getMillis()");
        assertEquals(0, record.getInstant().getNano() % NANOS_IN_MILLI,
                "record.getInstant().getNano() % NANOS_IN_MILLI");

        try {
            record.setInstant(null);
            throw new RuntimeException("Expected NullPointerException not thrown");
        } catch (NullPointerException x) {
            System.out.println("Got expected NPE when trying to call record.setInstant(null): " + x);
        }

        // This instant is the biggest for which toEpochMilli will not throw...
        final Instant max = Instant.ofEpochMilli(Long.MAX_VALUE).plusNanos(999_999L);
        record.setInstant(max);
        assertEquals(Long.MAX_VALUE / 1000L,
                     record.getInstant().getEpochSecond(),
                     "max instant seconds [record.getInstant().getEpochSecond()]");
        assertEquals(Long.MAX_VALUE,
                     record.getInstant().toEpochMilli(),
                     "max instant millis [record.getInstant().toEpochMilli()]");
        assertEquals(Long.MAX_VALUE, record.getMillis(),
                     "max instant millis [record.getMillis()]");
        assertEquals((Long.MAX_VALUE % 1000L)*1000_000L + 999_999L,
                     record.getInstant().getNano(),
                     "max instant nanos [record.getInstant().getNano()]");

        // Too big by 1 ns.
        final Instant tooBig = max.plusNanos(1L);
        try {
            record.setInstant(tooBig);
            throw new RuntimeException("Expected ArithmeticException not thrown");
        } catch (ArithmeticException x) {
            System.out.println("Got expected ArithmeticException when trying"
                    + " to call record.setInstant(Instant.ofEpochMilli(Long.MAX_VALUE)"
                    + ".plusNanos(999_999L).plusNanos(1L)): " + x);
        }

    }

}
