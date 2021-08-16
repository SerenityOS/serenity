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
import java.time.ZoneId;
import java.util.Base64;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @test
 * @bug 8072645
 * @summary tests the compatibility of LogRecord serial form between
 *          JDK 8 and JDK 9. Ideally this test should be run on both platforms.
 *          (It is designed to run on both).
 * @run main/othervm SerializeLogRecord
 * @author danielfuchs
 */
public class SerializeLogRecord {

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

        // Format the given logRecord using the SimpleFormatter
        SimpleFormatter formatter = new SimpleFormatter();
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

        // Format the deserialized LogRecord using the SimpleFormatter, and
        // check that the string representation obtained matches the string
        // representation of the original LogRecord
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
        //System.out.println(base64);
        //System.out.println();

        // Generates the Java Pseudo code that can be cut & pasted into
        // this test (see Jdk8SerializedLog and Jdk9SerializedLog below)
        final StringBuilder sb = new StringBuilder();
        sb.append("    /**").append('\n');
        sb.append("     * Base64 encoded string for LogRecord object.").append('\n');
        sb.append("     * Java version: ").append(System.getProperty("java.version")).append('\n');
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
        sb.append('\n');
        sb.append("    /**").append('\n');
        sb.append("     * SimpleFormatter output for LogRecord object.").append('\n');
        sb.append("     * Java version: ").append(System.getProperty("java.version")).append('\n');
        sb.append("     **/").append('\n');
        sb.append("    final String str = ").append("\n          ");
        sb.append("\"").append(str.replace("\n", "\\n")).append("\";\n");
        return sb.toString();
    }

    /**
     * An abstract class to test that a log record previously serialized on a
     * different java version can be deserialized in the current java version.
     * (see Jdk8SerializedLog and Jdk9SerializedLog below)
     */
    public abstract static class SerializedLog {
        public abstract String getBase64();
        public abstract String getString();

        /**
         * Deserializes the Base64 encoded string returned by {@link
         * #getBase64()}, format the obtained LogRecord using a
         * SimpleFormatter, and checks that the string representation obtained
         * matches the original string representation returned by {@link
         * #getString()}.
         */
        protected void dotest() {
            try {
                final String base64 = getBase64();
                final ByteArrayInputStream bais =
                        new ByteArrayInputStream(Base64.getDecoder().decode(base64));
                final ObjectInputStream ois = new ObjectInputStream(bais);
                final LogRecord record = (LogRecord)ois.readObject();
                final SimpleFormatter formatter = new SimpleFormatter();
                String expected = getString();
                String str2 = formatter.format(record);
                check(expected, str2);
                System.out.println(str2);
                System.out.println("PASSED: "+this.getClass().getName()+"\n");
            } catch (IOException | ClassNotFoundException x) {
                throw new RuntimeException(x);
            }
        }
        /**
         * Check that the actual String representation obtained matches the
         * expected String representation.
         * @param expected Expected String representation, as returned by
         *                 {@link #getString()}.
         * @param actual   Actual String representation obtained by formatting
         *                 the LogRecord obtained by the deserialization of the
         *                 bytes encoded in {@link #getBase64()}.
         */
        protected void check(String expected, String actual) {
            if (!expected.equals(actual)) {
                throw new RuntimeException(this.getClass().getName()
                    + " - Unexpected values in deserialized object:"
                    + "\n\tExpected:  " + expected
                    + "\n\tRetrieved: "+ actual);
            }
        }
    }

    public static class Jdk8SerializedLog extends SerializedLog {

        // Generated by generate() on JDK 8.
        // --------------------------------
        // BEGIN

        /**
         * Base64 encoded string for LogRecord object.
         * Java version: 1.8.0_11
         **/
        final String base64 =
              "rO0ABXNyABtqYXZhLnV0aWwubG9nZ2luZy5Mb2dSZWNvcmRKjVk982lRlgMACkoA"
            + "Bm1pbGxpc0oADnNlcXVlbmNlTnVtYmVySQAIdGhyZWFkSURMAAVsZXZlbHQAGUxq"
            + "YXZhL3V0aWwvbG9nZ2luZy9MZXZlbDtMAApsb2dnZXJOYW1ldAASTGphdmEvbGFu"
            + "Zy9TdHJpbmc7TAAHbWVzc2FnZXEAfgACTAAScmVzb3VyY2VCdW5kbGVOYW1lcQB+"
            + "AAJMAA9zb3VyY2VDbGFzc05hbWVxAH4AAkwAEHNvdXJjZU1ldGhvZE5hbWVxAH4A"
            + "AkwABnRocm93bnQAFUxqYXZhL2xhbmcvVGhyb3dhYmxlO3hwAAABSjUCgo0AAAAA"
            + "AAAAAAAAAAFzcgAXamF2YS51dGlsLmxvZ2dpbmcuTGV2ZWyOiHETUXM2kgIAA0kA"
            + "BXZhbHVlTAAEbmFtZXEAfgACTAAScmVzb3VyY2VCdW5kbGVOYW1lcQB+AAJ4cAAA"
            + "AyB0AARJTkZPdAAic3VuLnV0aWwubG9nZ2luZy5yZXNvdXJjZXMubG9nZ2luZ3QA"
            + "BHRlc3R0ABFKYXZhIFZlcnNpb246IHswfXBwcHB3BgEAAAAAAXQACDEuOC4wXzEx"
            + "eA==";

        /**
         * SimpleFormatter output for LogRecord object.
         * Java version: 1.8.0_11
         **/
        final String str =
              "Dec 10, 2014 4:22:44.621000000 PM test - INFO: Java Version: 1.8.0_11";
              //                    ^^^
              // Notice the milli second resolution above...

        // END
        // --------------------------------

        @Override
        public String getBase64() {
            return base64;
        }

        @Override
        public String getString() {
            return str;
        }

        public static void test() {
            new Jdk8SerializedLog().dotest();
        }
    }

    public static class Jdk9SerializedLog extends SerializedLog {

        // Generated by generate() on JDK 9.
        // --------------------------------
        // BEGIN

        /**
         * Base64 encoded string for LogRecord object.
         * Java version: 1.9.0-internal
         **/
        final String base64 =
              "rO0ABXNyABtqYXZhLnV0aWwubG9nZ2luZy5Mb2dSZWNvcmRKjVk982lRlgMAC0oA"
            + "Bm1pbGxpc0kADm5hbm9BZGp1c3RtZW50SgAOc2VxdWVuY2VOdW1iZXJJAAh0aHJl"
            + "YWRJREwABWxldmVsdAAZTGphdmEvdXRpbC9sb2dnaW5nL0xldmVsO0wACmxvZ2dl"
            + "ck5hbWV0ABJMamF2YS9sYW5nL1N0cmluZztMAAdtZXNzYWdlcQB+AAJMABJyZXNv"
            + "dXJjZUJ1bmRsZU5hbWVxAH4AAkwAD3NvdXJjZUNsYXNzTmFtZXEAfgACTAAQc291"
            + "cmNlTWV0aG9kTmFtZXEAfgACTAAGdGhyb3dudAAVTGphdmEvbGFuZy9UaHJvd2Fi"
            + "bGU7eHAAAAFLl3u6OAAOU/gAAAAAAAAAAAAAAAFzcgAXamF2YS51dGlsLmxvZ2dp"
            + "bmcuTGV2ZWyOiHETUXM2kgIAA0kABXZhbHVlTAAEbmFtZXEAfgACTAAScmVzb3Vy"
            + "Y2VCdW5kbGVOYW1lcQB+AAJ4cAAAAyB0AARJTkZPdAAic3VuLnV0aWwubG9nZ2lu"
            + "Zy5yZXNvdXJjZXMubG9nZ2luZ3QABHRlc3R0ABFKYXZhIFZlcnNpb246IHswfXBw"
            + "cHB3BgEAAAAAAXQADjEuOS4wLWludGVybmFseA==";

        /**
         * SimpleFormatter output for LogRecord object.
         * Java version: 1.9.0-internal
         **/
        final String str =
              "Feb 17, 2015 12:20:43.192939000 PM test - INFO: Java Version: 1.9.0-internal";
              //                       ^^^
              // Notice the micro second resolution above...

        // END
        // --------------------------------

        @Override
        public String getBase64() {
            return base64;
        }

        @Override
        public String getString() {
            return str;
        }

        @Override
        protected void check(String expected, String actual) {
            if (System.getProperty("java.version").startsWith("1.8")) {
                // If we are in JDK 8 and print a log record serialized in JDK 9,
                // then we won't be able to print anything below the millisecond
                // precision, since that hasn't been implemented in JDK 8.
                // Therefore - we need to replace anything below millseconds by
                // zeroes in the expected string (which was generated on JDK 9).
                Pattern pattern = Pattern.compile("^"
                        + "(.*\\.[0-9][0-9][0-9])" // group1: everything up to milliseconds
                        + "([0-9][0-9][0-9][0-9][0-9][0-9])" // group 2: micros and nanos
                        + "(.* - .*)$"); // group three: all the rest...
                Matcher matcher = pattern.matcher(expected);
                if (matcher.matches()) {
                    expected = matcher.group(1) + "000000" + matcher.group(3);
                }
            }
            super.check(expected, actual);
        }

        public static void test() {
            new Jdk9SerializedLog().dotest();
        }
    }

    public static void generate() {
        try {
            LogRecord record = new LogRecord(Level.INFO, "Java Version: {0}");
            record.setLoggerName("test");
            record.setParameters(new Object[] {System.getProperty("java.version")});
            System.out.println(generate(record));
        } catch (IOException | ClassNotFoundException x) {
            throw new RuntimeException(x);
        }
    }

    static enum TestCase { GENERATE, TESTJDK8, TESTJDK9 };

    public static void main(String[] args) {
        // Set the locale and time zone to make sure we won't depend on the
        // test env - in particular we don't want to depend on the
        // time zone in which the test machine might be located.
        // So we're gong to use Locale English and Time Zone UTC for this test.
        // (Maybe that should be Locale.ROOT?)
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")));

        // Set the format property to make sure we always have the nanos, and
        // to make sure it's the same format than what we used when
        // computing the formatted string for Jdk8SerializedLog and
        // Jdk9SerializedLog above.
        //
        // If you change the formatting, then you will need to regenerate
        // the data for Jdk8SerializedLog and Jdk9SerializedLog.
        //
        // To do that - just run this test on JDK 8, and cut & paste the
        // pseudo code printed by generate() into Jdk8SerializedLog.
        // Then run this test again on JDK 9, and cut & paste the
        // pseudo code printed by generate() into Jdk9SerializedLog.
        // [Note: you can pass GENERATE as single arg to main() to avoid
        //        running the actual test]
        // Finally run the test again to check that it still passes after
        // your modifications.
        //
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS.%1$tN %1$Tp %2$s - %4$s: %5$s%6$s");

        // If no args, then run everything....
        if (args == null || args.length == 0) {
            args = new String[] { "GENERATE", "TESTJDK8", "TESTJDK9" };
        }

        // Run the specified test case(s)
        Stream.of(args).map(x -> TestCase.valueOf(x)).forEach((x) -> {
            switch(x) {
                case GENERATE: generate(); break;
                case TESTJDK8: Jdk8SerializedLog.test(); break;
                case TESTJDK9: Jdk9SerializedLog.test(); break;
            }
        });
    }
}
