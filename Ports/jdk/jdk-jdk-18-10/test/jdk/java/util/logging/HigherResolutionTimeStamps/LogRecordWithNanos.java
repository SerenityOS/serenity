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
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * @test
 * @bug 8072645
 * @summary tests that LogRecord has nanos...
 * @run main LogRecordWithNanos
 * @author danielfuchs
 */
public class LogRecordWithNanos {

    static final int MILLIS_IN_SECOND = 1000;
    static final int NANOS_IN_MILLI = 1000_000;
    static final int NANOS_IN_SECOND = 1000_000_000;

    static final boolean verbose = false;

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

    /**
     * Serializes a log record, then deserializes it and check that both
     * records match.
     * @param record the log record to serialize & deserialize.
     * @throws IOException Unexpected.
     * @throws ClassNotFoundException  Unexpected.
     */
    public static void test(LogRecord record)
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
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
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
        assertEquals(record.getMillis(),
                record.getInstant().toEpochMilli(),
                "getMillis()/getInstant().toEpochMilli()");
        assertEquals(record2.getMillis(),
                record2.getInstant().toEpochMilli(),
                "getMillis()/getInstant().toEpochMilli()");
        assertEquals((record.getMillis()%MILLIS_IN_SECOND)*NANOS_IN_MILLI
                + (record.getInstant().getNano() % NANOS_IN_MILLI),
                record.getInstant().getNano(),
                "record.getMillis()%1000)*1000_000"
                + " + record.getInstant().getNano()%1000_000 / getInstant().getNano()");
        assertEquals((record2.getMillis()%MILLIS_IN_SECOND)*NANOS_IN_MILLI
                + (record2.getInstant().getNano() % NANOS_IN_MILLI),
                record2.getInstant().getNano(),
                "record2.getMillis()%1000)*1000_000"
                + " + record2.getInstant().getNano()%1000_000 / getInstant().getNano()");

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
        for (int i=0; i<1000; i++) {
            LogRecord record = new LogRecord(Level.INFO, "Java Version: {0}");
            record.setLoggerName("test");
            record.setParameters(new Object[] {System.getProperty("java.version")});
            if (record.getInstant().getNano() % 1000_000 != 0) {
                count++;
            }
            test(record);
        }
        if (count == 0) {
            throw new RuntimeException("Expected at least one record to have nanos");
        }
        System.out.println(count + "/1000 records had nano adjustment.");
    }

}
