/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import static org.testng.Assert.*;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import jdk.test.lib.process.ProcessTools;

import jdk.test.lib.hexdump.HexPrinter;
import jdk.test.lib.hexdump.HexPrinter.Formatters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Locale;

import jtreg.SkippedException;

/*
 * @test
 * @library /test/lib
 * @build jdk.test.lib.process.ProcessTools jdk.test.lib.hexdump.HexPrinter
 * @run testng ReaderWriterTest
 */

@Test
public class ReaderWriterTest {

    static final String ASCII = "ASCII: \u0000_A-Z_a-Z_\u007C_\u007D_\u007E_\u007F_;";
    static final String ISO_8859_1 = " Symbols: \u00AB_\u00BB_\u00fc_\u00fd_\u00fe_\u00ff;";
    static final String FRACTIONS = " Fractions: \u00bc_\u00bd_\u00be_\u00bf;";

    public static final String TESTCHARS = "OneWay: " + ASCII + ISO_8859_1 + FRACTIONS;
    public static final String ROUND_TRIP_TESTCHARS = "RoundTrip: " + ASCII + ISO_8859_1 + FRACTIONS;

    @DataProvider(name="CharsetCases")
    static Object[][] charsetCases() {
        return new Object[][] {
                {"UTF-8"},
                {"ISO8859-1"},
                {"US-ASCII"},
        };
    }

    /**
     * Test the defaults case of native.encoding.  No extra command line flags or switches.
     */
    @Test
    void testCaseNativeEncoding() throws IOException {
        String nativeEncoding = System.getProperty("native.encoding");
        Charset cs = Charset.forName(nativeEncoding);
        System.out.println("Native.encoding Charset: " + cs);

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("ReaderWriterTest$ChildWithCharset");
        Process p = pb.start();
        writeTestChars(p.outputWriter());
        checkReader(p.inputReader(), cs, "Out");
        checkReader(p.errorReader(), cs, "Err");
        try {
            int exitValue = p.waitFor();
            if (exitValue != 0)
                System.out.println("exitValue: " + exitValue);
        } catch (InterruptedException ie) {
            Assert.fail("waitFor interrupted");
        }
    }

    /**
     * Test that redirects of input and error streams result in Readers that are empty.
     * Test that when the output to a process is redirected, the writer acts as
     * a null stream and throws an exception as expected for a null output stream
     * as specified by ProcessBuilder.
     */
    @Test
    void testRedirects() throws IOException {
        String nativeEncoding = System.getProperty("native.encoding");
        Charset cs = Charset.forName(nativeEncoding);
        System.out.println("Native.encoding Charset: " + cs);

        Path inPath = Path.of("InFile.tmp");
        BufferedWriter inWriter = Files.newBufferedWriter(inPath);
        inWriter.close();

        Path outPath = Path.of("OutFile.tmp");
        Path errorPath = Path.of("ErrFile.tmp");

        for (int errType = 1; errType < 4; errType++) {
            // Three cases to test for which the error stream is empty
            // 1: redirectErrorStream(false); redirect of errorOutput to a file
            // 2: redirectErrorStream(true); no redirect of errorOutput
            // 3: redirectErrorStream(true); redirect of errorOutput to a file

            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("ReaderWriterTest$ChildWithCharset");
            pb.redirectInput(inPath.toFile());
            pb.redirectOutput(outPath.toFile());
            if (errType == 1 || errType == 3) {
                pb.redirectError(errorPath.toFile());
            }
            if (errType == 2 || errType == 3) {
                pb.redirectErrorStream(true);
            }
            Process p = pb.start();
            // Output has been redirected to a null stream; success is IOException on the write
            try {
                BufferedWriter wr = p.outputWriter();
                wr.write("X");
                wr.flush();
                Assert.fail("writing to null stream should throw IOException");
            } catch (IOException ioe) {
                // Normal, A Null output stream is closed when created.
            }

            // InputReader should be empty; and at EOF
            BufferedReader inputReader = p.inputReader();
            int ch = inputReader.read();
            Assert.assertEquals(ch, -1, "inputReader not at EOF: ch: " + (char)ch);

            // InputReader should be empty; and at EOF
            BufferedReader errorReader = p.errorReader();
            ch = errorReader.read();
            Assert.assertEquals(ch, -1, "errorReader not at EOF: ch: " + (char)ch);

            try {
                int exitValue = p.waitFor();
                if (exitValue != 0) System.out.println("exitValue: " + exitValue);
            } catch (InterruptedException ie) {
                Assert.fail("waitFor interrupted");
            }
        }
    }

    /**
     * Write the test characters to the child using the Process.outputWriter.
     * @param writer the Writer
     * @throws IOException if an I/O error occurs
     */
    private static void writeTestChars(Writer writer) throws IOException {
        // Write the test data to the child
        try (writer) {
            writer.append(ROUND_TRIP_TESTCHARS);
            writer.append(System.lineSeparator());
        }
    }

    /**
     * Test a child with a character set.
     * A Process is spawned; characters are written to and read from the child
     * using the character set and compared.
     *
     * @param encoding a charset name
     */
    @Test(dataProvider = "CharsetCases", enabled = true)
    void testCase(String encoding) throws IOException {
        Charset cs = null;
        try {
            cs = Charset.forName(encoding);
            System.out.println("Charset: " + cs);
        } catch (UnsupportedCharsetException use) {
            throw new SkippedException("Charset not supported: " + encoding);
        }
        String cleanCSName = cleanCharsetName(cs);

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-Dsun.stdout.encoding=" + cleanCSName,     // Encode in the child using the charset
                "-Dsun.stderr.encoding=" + cleanCSName,
                "ReaderWriterTest$ChildWithCharset");

        Process p = pb.start();
        // Write the test data to the child
        writeTestChars(p.outputWriter(cs));
        checkReader(p.inputReader(cs), cs, "Out");
        checkReader(p.errorReader(cs), cs, "Err");
        try {
            int exitValue = p.waitFor();
            if (exitValue != 0)
                System.out.println("exitValue: " + exitValue);
        } catch (InterruptedException ie) {

        }
    }

    /**
     * Test passing null when a charset is expected
     * @throws IOException if an I/O error occurs; not expected
     */
    @Test
    void testNullCharsets()  throws IOException {
        // Launch a child; its behavior is not interesting and is ignored
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "ReaderWriterTest$ChildWithCharset");

        Process p = pb.start();
        try {
            writeTestChars(p.outputWriter(null));
            Assert.fail("Process.outputWriter(null) did not throw NPE");
        } catch (NullPointerException npe) {
            // expected, ignore
        }
        try {
            checkReader(p.inputReader(null), null, "Out");
            Assert.fail("Process.inputReader(null) did not throw NPE");
        } catch (NullPointerException npe) {
            // expected, ignore
        }
        try {
            checkReader(p.errorReader(null), null, "Err");
            Assert.fail("Process.errorReader(null) did not throw NPE");
        } catch (NullPointerException npe) {
            // expected, ignore
        }

        p.destroyForcibly();
        try {
            // Collect the exit status to cleanup after the process; but ignore it
            p.waitFor();
        } catch (InterruptedException ie) {
            // Ignored
        }
    }

    /**
     * Test passing different charset on multiple calls when the same charset is expected.
     * @throws IOException if an I/O error occurs; not expected
     */
    @Test
    void testIllegalArgCharsets()  throws IOException {
        String nativeEncoding = System.getProperty("native.encoding");
        Charset cs = Charset.forName(nativeEncoding);
        System.out.println("Native.encoding Charset: " + cs);
        Charset otherCharset = cs.equals(StandardCharsets.UTF_8)
                ? StandardCharsets.ISO_8859_1
                : StandardCharsets.UTF_8;

        // Launch a child; its behavior is not interesting and is ignored
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "ReaderWriterTest$ChildWithCharset");

        Process p = pb.start();
        try {
            var writer = p.outputWriter(cs);
            writer = p.outputWriter(cs);        // try again with same
            writer = p.outputWriter(otherCharset);  // this should throw
            Assert.fail("Process.outputWriter(otherCharset) did not throw IllegalStateException");
        } catch (IllegalStateException ile) {
            // expected, ignore
            System.out.println(ile);
        }
        try {
            var reader = p.inputReader(cs);
            reader = p.inputReader(cs);             // try again with same
            reader = p.inputReader(otherCharset);   // this should throw
            Assert.fail("Process.inputReader(otherCharset) did not throw IllegalStateException");
        } catch (IllegalStateException ile) {
            // expected, ignore
            System.out.println(ile);
        }
        try {
            var reader = p.errorReader(cs);
            reader = p.errorReader(cs);             // try again with same
            reader = p.errorReader(otherCharset);   // this should throw
            Assert.fail("Process.errorReader(otherCharset) did not throw IllegalStateException");
        } catch (IllegalStateException ile) {
            // expected, ignore
            System.out.println(ile);
        }

        p.destroyForcibly();
        try {
            // Collect the exit status to cleanup after the process; but ignore it
            p.waitFor();
        } catch (InterruptedException ie) {
            // Ignored
        }
    }

    private static void checkReader(BufferedReader reader, Charset cs, String label) throws IOException {
        try (BufferedReader in = reader) {
            String prefix = "    " + label + ": ";
            String firstline = in.readLine();
            System.out.append(prefix).println(firstline);
            String secondline = in.readLine();
            System.out.append(prefix).println(secondline);
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                System.out.append(prefix).append(line);
                System.out.println();
            }
            ByteBuffer bb = cs.encode(TESTCHARS);
            String reencoded = cs.decode(bb).toString();
            if (!firstline.equals(reencoded))
                diffStrings(firstline, reencoded);
            assertEquals(firstline, reencoded, label + " Test Chars");

            bb = cs.encode(ROUND_TRIP_TESTCHARS);
            reencoded = cs.decode(bb).toString();
            if (!secondline.equals(reencoded))
                diffStrings(secondline, reencoded);
            assertEquals(secondline, reencoded, label + " Round Trip Test Chars");
        }
    }

    /**
     * A cleaned up Charset name that is suitable for Linux LANG environment variable.
     * If there are two '-'s the first one is removed.
     * @param cs a Charset
     * @return the cleanedup Charset name
     */
    private static String cleanCharsetName(Charset cs) {
        String name = cs.name();
        int ndx = name.indexOf('-');
        if (ndx >= 0 && name.indexOf('-', ndx + 1) >= 0) {
            name = name.substring(0, ndx) + name.substring(ndx + 1);
        }
        return name;
    }

    private static void diffStrings(String actual, String expected) {
        if (actual.equals(expected))
            return;
        int lenDiff = expected.length() - actual.length();
        if (lenDiff != 0)
            System.out.println("String lengths:  " + actual.length() + " != " + expected.length());
        int first;  // find first mismatched character
        for (first = 0; first < Math.min(actual.length(), expected.length()); first++) {
            if (actual.charAt(first) != expected.charAt(first))
                break;
        }
        int last;
        for (last = actual.length() - 1; last >= 0 && (last + lenDiff) >= 0; last--) {
            if (actual.charAt(last) != expected.charAt(last + lenDiff))
                break;      // last mismatched character
        }
        System.out.printf("actual vs expected[%3d]: 0x%04x != 0x%04x%n", first, (int)actual.charAt(first), (int)expected.charAt(first));
        System.out.printf("actual vs expected[%3d]: 0x%04x != 0x%04x%n", last, (int)actual.charAt(last), (int)expected.charAt(last));
        System.out.printf("actual  [%3d-%3d]: %s%n", first, last, actual.substring(first, last+1));
        System.out.printf("expected[%3d-%3d]: %s%n", first, last, expected.substring(first, last + lenDiff + 1));
    }

    static class ChildWithCharset {
        public static void main(String[] args) {
            String nativeEncoding = System.getProperty("native.encoding");
            System.out.println(TESTCHARS);
            byte[] bytes = null;
            try {
                bytes = System.in.readAllBytes();
                System.out.write(bytes);    // echo bytes back to parent on stdout
            } catch (IOException ioe) {
                ioe.printStackTrace();      // Seen by the parent
            }
            System.out.println("native.encoding: " + nativeEncoding);
            System.out.println("sun.stdout.encoding: " + System.getProperty("sun.stdout.encoding"));
            System.out.println("LANG: " + System.getenv().get("LANG"));

            System.err.println(TESTCHARS);
            try {
                System.err.write(bytes);    // echo bytes back to parent on stderr
            } catch (IOException ioe) {
                ioe.printStackTrace();      // Seen by the parent
            }
            System.err.println("native.encoding: " + nativeEncoding);
            System.err.println("sun.stderr.encoding: " + System.getProperty("sun.stderr.encoding"));
        }
    }
}
