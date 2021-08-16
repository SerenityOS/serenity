/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.XMLFormatter;
import java.util.stream.Stream;

/**
 * @test
 * @bug 8245302
 * @summary tests the deprecation of threadID and a new field longThreadID,
 * test should be run on jdk16 and subsequent versions
 * @run main/othervm SerializeLogRecordTest
 */
public class SerializeLogRecordTest {

    /**
     * Serializes a log record, encode the serialized bytes in base 64, and
     * prints pseudo java code that can be cut and pasted into this test.
     * @param record the log record to serialize, encode in base 64, and for
     *               which test data will be generated.
     * @return A string containing the generated pseudo java code.
     * @throws IOException Unexpected.
     * @throws ClassNotFoundException  Unexpected.
     */
    public static String generate(LogRecord record) throws IOException, ClassNotFoundException {

        XMLFormatter formatter = new XMLFormatter();
        String str = formatter.format(record);

        // Serialize the given LogRecord
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(record);
        oos.flush();
        oos.close();

        // Now we're going to perform a number of smoke tests before
        // generating the Java pseudo code.
        //
        // First checks that the log record can be deserialized
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bais);
        final LogRecord record2 = (LogRecord)ois.readObject();

        String str2 = formatter.format(record2);

        if (!str.equals(str2)) throw new RuntimeException("Unexpected values in deserialized object:"
            + "\n\tExpected:  " + str
            + "\n\tRetrieved: "+str);

        // Now get a Base64 string representation of the serialized bytes.
        final String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

        // Check that we can deserialize a log record from the Base64 string
        // representation we just computed.
        final ByteArrayInputStream bais2 = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
        final ObjectInputStream ois2 = new ObjectInputStream(bais2);
        final LogRecord record3 = (LogRecord)ois2.readObject();

        // Format the new deserialized LogRecord using the SimpleFormatter, and
        // check that the string representation obtained matches the string
        // representation of the original LogRecord
        String str3 = formatter.format(record3);
        if (!str.equals(str3)) throw new RuntimeException("Unexpected values in deserialized object:"
            + "\n\tExpected:  " + str
            + "\n\tRetrieved: "+str);

        // Generates the Java Pseudo code that can be cut & pasted into
        // this test (see Jdk8SerializedLog and Jdk9SerializedLog below)
        final StringBuilder sb = new StringBuilder();
        sb.append("    /**").append('\n');
        sb.append("     * Base64 encoded string for LogRecord object.").append('\n');
        sb.append("     * Java version: ").append(System.getProperty("java.version")).append('\n');
        sb.append("     * threadID: ").append(record.getThreadID()).append('\n');
        sb.append("     * longThreadID: ").append(record.getLongThreadID()).append('\n');
        sb.append("     **/").append('\n');
        sb.append("    final String base64 = ").append("\n          ");
        final int last = base64.length() - 1;
        for (int i=0; i<base64.length();i++) {
            if (i%64 == 0) sb.append("\"");
            sb.append(base64.charAt(i));
            if (i%64 == 63 || i == last) {
                sb.append("\"");
                if (i == last) sb.append(";\n");
                else sb.append("\n        + ");
            }
        }
        return sb.toString();
    }

    /**
     * An abstract class to test that a log record previously serialized on a
     * different java version can be deserialized in the current java version.
     * (see Jdk11SerializedLog and Jdk16SerializedLog below)
     */
    public abstract static class SerializedLog {
        public abstract String getBase64();

        /**
         * Deserializes the Base64 encoded string returned by {@link
         * #getBase64()}, and checks that the string representation obtained
         * matches the original string representation returned by
         */
        protected void dotest() {
            try {
                final String base64 = getBase64();
                final ByteArrayInputStream bais =
                        new ByteArrayInputStream(Base64.getDecoder().decode(base64));
                final ObjectInputStream ois = new ObjectInputStream(bais);
                final LogRecord record = (LogRecord)ois.readObject();
                check(record);
                System.out.println("PASSED: "+this.getClass().getName()+"\n");
            } catch (IOException | ClassNotFoundException x) {
                throw new RuntimeException(x);
            }
        }
        /**
         * Check that the values of threadID and longThreadID
         * are correct and consistent when de-serialized from
         * earlier jdk versions(jdk11)
         */
        protected void check(LogRecord r1) {
            XMLFormatter formatter = new XMLFormatter();
            int check = Integer.MAX_VALUE - 10;
            String str = formatter.format(r1);
            String loggerName = r1.getLoggerName();

            if (loggerName.equals("test2")) {
                int id = -2147483639;
                if (r1.getLongThreadID() != Integer.MAX_VALUE + 10L || r1.getThreadID() != id) {
                    throw new RuntimeException(this.getClass().getName());
                }
                else {
                    System.out.println("Test Integer Check Passed");
                }

            }

            if (loggerName.equals("test1") || loggerName.equals("test")) {
                if (loggerName.equals("test1")) {
                    check = Integer.MAX_VALUE / 2 - 20;
                }

                int tid = r1.getThreadID();
                long longThreadID = r1.getLongThreadID();
                if (tid != check || longThreadID != check || !str.contains("<thread>" + String.valueOf(longThreadID))) {
                    throw new RuntimeException(this.getClass().getName());
                }
                else {
                    System.out.println("Test Integer Check Passed");
                }
            }
        }
    }

    public static void generate() {
        try {
            LogRecord record = new LogRecord(Level.INFO, "Java Version: {0}");
            record.setLoggerName("test");
            record.setThreadID(Integer.MAX_VALUE - 10);
            record.setParameters(new Object[] {System.getProperty("java.version")});
            System.out.println(generate(record));
            LogRecord record1 = new LogRecord(Level.INFO, "Java Version: {0}");
            record1.setLoggerName("test1");
            record1.setLongThreadID(Integer.MAX_VALUE/2 - 20);
            record1.setParameters(new Object[] {System.getProperty("java.version")});
            System.out.println(generate(record1));
            LogRecord record2 = new LogRecord(Level.INFO, "Java Version: {0}");
            record2.setLoggerName("test2");
            record2.setLongThreadID(Integer.MAX_VALUE + 10L);
            record2.setParameters(new Object[] {System.getProperty("java.version")});
            System.out.println(generate(record2));
        } catch (IOException | ClassNotFoundException x) {
            throw new RuntimeException(x);
        }
    }

    public static class Jdk11SerializedLog extends SerializedLog {

        /**
         * Base64 encoded string for LogRecord object.
         * It is generated using generate method and
         * can be copy pasted as base64 value, for generating
         * this value, execute this class with an argument value
         * generate and the test will print out the base64 value
         * that can be copy pasted below
         * Java version: 11.0.6
         **/
        final String base64 =
            "rO0ABXNyABtqYXZhLnV0aWwubG9nZ2luZy5Mb2dSZWNvcmRKjVk982lRlgMAC0oA"
                + "Bm1pbGxpc0kADm5hbm9BZGp1c3RtZW50SgAOc2VxdWVuY2VOdW1iZXJJAAh0aHJl"
                + "YWRJREwABWxldmVsdAAZTGphdmEvdXRpbC9sb2dnaW5nL0xldmVsO0wACmxvZ2dl"
                + "ck5hbWV0ABJMamF2YS9sYW5nL1N0cmluZztMAAdtZXNzYWdlcQB+AAJMABJyZXNv"
                + "dXJjZUJ1bmRsZU5hbWVxAH4AAkwAD3NvdXJjZUNsYXNzTmFtZXEAfgACTAAQc291"
                + "cmNlTWV0aG9kTmFtZXEAfgACTAAGdGhyb3dudAAVTGphdmEvbGFuZy9UaHJvd2Fi"
                + "bGU7eHAAAAFycRovVgAFN/AAAAAAAAAAAH////VzcgAXamF2YS51dGlsLmxvZ2dp"
                + "bmcuTGV2ZWyOiHETUXM2kgIAA0kABXZhbHVlTAAEbmFtZXEAfgACTAAScmVzb3Vy"
                + "Y2VCdW5kbGVOYW1lcQB+AAJ4cAAAAyB0AARJTkZPdAAic3VuLnV0aWwubG9nZ2lu"
                + "Zy5yZXNvdXJjZXMubG9nZ2luZ3QABHRlc3R0ABFKYXZhIFZlcnNpb246IHswfXBw"
                + "cHB3BgEAAAAAAXQABjExLjAuNng=";


        @Override
        public String getBase64() {
            return base64;
        }

        @Override
        protected void check(LogRecord r1) {
            super.check(r1);
        }

        public static void test() {
            new Jdk11SerializedLog().dotest();
        }
    }

    public static class Jdk16SerializedLog extends SerializedLog {

        /**
         * Base64 encoded string for LogRecord object.
         * It is generated using generate method and
         * can be copy pasted as base64 value, for generating
         * this value, execute this class with an argument value
         * generate and the test will print out the base64 value
         * that can be copy pasted below
         * Java version: 16-internal
         **/
        final String base64 =
            "rO0ABXNyABtqYXZhLnV0aWwubG9nZ2luZy5Mb2dSZWNvcmRKjVk982lRlgMADEoA"
                + "DGxvbmdUaHJlYWRJREoABm1pbGxpc0kADm5hbm9BZGp1c3RtZW50SgAOc2VxdWVu"
                + "Y2VOdW1iZXJJAAh0aHJlYWRJREwABWxldmVsdAAZTGphdmEvdXRpbC9sb2dnaW5n"
                + "L0xldmVsO0wACmxvZ2dlck5hbWV0ABJMamF2YS9sYW5nL1N0cmluZztMAAdtZXNz"
                + "YWdlcQB+AAJMABJyZXNvdXJjZUJ1bmRsZU5hbWVxAH4AAkwAD3NvdXJjZUNsYXNz"
                + "TmFtZXEAfgACTAAQc291cmNlTWV0aG9kTmFtZXEAfgACTAAGdGhyb3dudAAVTGph"
                + "dmEvbGFuZy9UaHJvd2FibGU7eHAAAAAAgAAACQAAAXLMALDdAAS+2AAAAAAAAAAC"
                + "gAAACXNyABdqYXZhLnV0aWwubG9nZ2luZy5MZXZlbI6IcRNRczaSAgADSQAFdmFs"
                + "dWVMAARuYW1lcQB+AAJMABJyZXNvdXJjZUJ1bmRsZU5hbWVxAH4AAnhwAAADIHQA"
                + "BElORk90ACJzdW4udXRpbC5sb2dnaW5nLnJlc291cmNlcy5sb2dnaW5ndAAFdGVz"
                + "dDJ0ABFKYXZhIFZlcnNpb246IHswfXBwcHB3BgEAAAAAAXQACzE2LWludGVybmFs"
                + "eA==";


        @Override
        public String getBase64() {
            return base64;
        }

        @Override
        protected void check(LogRecord r1) {
            super.check(r1);
        }

        public static void test() {
            new Jdk16SerializedLog().dotest();
        }
    }

    static enum TestCase { GENERATE, TESTJDK11, TESTJDK16 };

    public static void main(String[] args) {


        // If no args, then run everything....
        if (args == null || args.length == 0) {
            args = new String[] { "GENERATE", "TESTJDK11", "TESTJDK16" };
        }

        // Run the specified test case(s)
        Stream.of(args).map(x -> TestCase.valueOf(x)).forEach((x) -> {
            switch(x) {
                  case GENERATE:  generate(); break;
                  case TESTJDK11: Jdk11SerializedLog.test(); break;
                  case TESTJDK16: Jdk16SerializedLog.test(); break;
            }
        });
    }
}
