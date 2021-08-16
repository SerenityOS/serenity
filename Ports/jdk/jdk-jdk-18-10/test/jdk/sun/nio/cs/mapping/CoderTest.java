/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4691554 6221056 6380723 6404504 6419565 6529796
   @summary Test the supported New I/O coders
   @modules jdk.charsets
 */

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.regex.*;

public class CoderTest {
    private static final int BUFSIZ = 8192;     // Initial buffer size
    private static final int MAXERRS = 10;      // Errors reported per test

    private static final String testRootDir
        = System.getProperty("test.src", ".");
    private static final PrintStream log = System.out;

    // Set by -v on the command line
    private static boolean verbose = false;

    // Test modes
    private static final int ROUNDTRIP = 0;
    private static final int ENCODE = 1;
    private static final int DECODE = 2;

    private static boolean shiftHackDBCS = false;

    // File extensions, indexed by test mode
    private static final String[] extension
        = new String[] { ".b2c",
                         ".c2b-irreversible",
                         ".b2c-irreversible" };


    // Utilities
    private static ByteBuffer expand(ByteBuffer bb) {
        ByteBuffer nbb = ByteBuffer.allocate(bb.capacity() * 2);
        bb.flip();
        nbb.put(bb);
        return nbb;
    }

    private static CharBuffer expand(CharBuffer cb) {
        CharBuffer ncb = CharBuffer.allocate(cb.capacity() * 2);
        cb.flip();
        ncb.put(cb);
        return ncb;
    }

    private static byte[] parseBytes(String s) {
        int nb = s.length() / 2;
        byte[] bs = new byte[nb];
        for (int i = 0; i < nb; i++) {
            int j = i * 2;
            if (j + 2 > s.length())
                throw new RuntimeException("Malformed byte string: " + s);
            bs[i] = (byte)Integer.parseInt(s.substring(j, j + 2), 16);
        }
        return bs;
    }

    private static String printBytes(byte[] bs) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bs.length; i++) {
            sb.append(Integer.toHexString((bs[i] >> 4) & 0xf));
            sb.append(Integer.toHexString((bs[i] >> 0) & 0xf));
        }
        return sb.toString();
    }

    private static String printCodePoint(int cp) {
        StringBuffer sb = new StringBuffer();
        sb.append("U+");
        if (cp > 0xffff)
            sb.append(Integer.toHexString((cp >> 16) & 0xf));
        sb.append(Integer.toHexString((cp >> 12) & 0xf));
        sb.append(Integer.toHexString((cp >> 8) & 0xf));
        sb.append(Integer.toHexString((cp >> 4) & 0xf));
        sb.append(Integer.toHexString((cp >> 0) & 0xf));
        return sb.toString();
    }

    private static int getCodePoint(CharBuffer cb) {
        char c = cb.get();
        if (Character.isHighSurrogate(c))
            return Character.toCodePoint(c, cb.get());
        else
            return c;
    }

    private static String plural(int n) {
        return (n == 1 ? "" : "s");
    }

    static class Entry {
        byte[] bb;
        int cp;
        int cp2;
    }

    public static class Parser {
        static Pattern p = Pattern.compile("(0[xX])?(00)?([0-9a-fA-F]+)\\s+(0[xX])?([0-9a-fA-F]+)(\\+0x([0-9a-fA-F]+))?\\s*");
        static final int gBS = 1;
        static final int gCP = 2;
        static final int gCP2 = 3;

        boolean isStateful = false;
        BufferedReader reader;
        boolean closed;
        Matcher matcher;

        public Parser (InputStream in)
            throws IOException
        {
            this.reader = new BufferedReader(new InputStreamReader(in));
            this.closed = false;
            this.matcher = p.matcher("");
        }

        public boolean isStateful() {
            return isStateful;
        }

        protected boolean isDirective(String line) {
            // Stateful DBCS encodings need special treatment
            if (line.startsWith("#STATEFUL")) {
                return isStateful = true;
            }
            return line.startsWith("#");
        }

        protected Entry parse(Matcher m, Entry e) {
            e.bb = parseBytes(m.group(3));
            e.cp = Integer.parseInt(m.group(5), 16);
            if (m.group(7) != null)
                e.cp2 = Integer.parseInt(m.group(7), 16);
            else
                e.cp2 = 0;
            return e;
        }

        public Entry next() throws Exception {
            return next(new Entry());
        }

        // returns null and closes the input stream if the eof has beenreached.
        public Entry next(Entry mapping) throws Exception {
            if (closed)
                return null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (isDirective(line))
                    continue;
                matcher.reset(line);
                if (!matcher.lookingAt()) {
                    //System.out.println("Missed: " + line);
                    continue;
                }
                return parse(matcher, mapping);
            }
            reader.close();
            closed = true;
            return null;
        }
     }

    // CoderTest
    private String encoding;
    private Charset cs;
    private CharsetDecoder decoder = null;
    private CharsetEncoder encoder = null;

    private CoderTest(String enc) throws Exception {
        encoding = enc;
        cs = Charset.forName(enc);
        decoder = cs.newDecoder();
        encoder = cs.newEncoder();
    }

    private class Test {
        // An instance of this class tests all mappings for
        // a particular bytesPerChar value
        private int bytesPerChar;

        // Reference data from .b2c file
        private ByteBuffer refBytes = ByteBuffer.allocate(BUFSIZ);
        private CharBuffer refChars = CharBuffer.allocate(BUFSIZ);

        private ByteBuffer dRefBytes = ByteBuffer.allocateDirect(BUFSIZ);
        private CharBuffer dRefChars = ByteBuffer.allocateDirect(BUFSIZ*2).asCharBuffer();

        private Test(int bpc) {
            bytesPerChar = bpc;
        }

        private void put(byte[] bs, char[] cc) {
            if (bs.length != bytesPerChar)
                throw new IllegalArgumentException(bs.length
                                                   + " != "
                                                   + bytesPerChar);
            if (refBytes.remaining() < bytesPerChar)
                refBytes = expand(refBytes);
            refBytes.put(bs);
            if (refChars.remaining() < cc.length)
                refChars = expand(refChars);
            refChars.put(cc);
        }

        private boolean decode(ByteBuffer refByte, CharBuffer refChars)
            throws Exception {
            log.println("    decode" + (refByte.isDirect()?" (direct)":""));
            CharBuffer out = decoder.decode(refBytes);

            refBytes.rewind();
            byte[] bs = new byte[bytesPerChar];
            int e = 0;

            while (refBytes.hasRemaining()) {
                refBytes.get(bs);
                int rcp = getCodePoint(refChars);
                int ocp = getCodePoint(out);
                if (rcp != ocp) {
                    log.println("      Error: "
                                + printBytes(bs)
                                + " --> "
                                + printCodePoint(ocp)
                                + ", expected "
                                + printCodePoint(rcp));
                    if (++e >= MAXERRS) {
                        log.println("      Too many errors, giving up");
                        break;
                    }
                }
                if (verbose) {
                    log.println("      "
                                + printBytes(bs)
                                + " --> "
                                + printCodePoint(rcp));
                }
            }
            if (e == 0 && (refChars.hasRemaining() || out.hasRemaining())) {
                // Paranoia: Didn't consume everything
                throw new IllegalStateException();
            }
            refBytes.rewind();
            refChars.rewind();
            return (e == 0);
        }

        private boolean encode(ByteBuffer refByte, CharBuffer refChars)
            throws Exception {
            log.println("    encode" + (refByte.isDirect()?" (direct)":""));
            ByteBuffer out = encoder.encode(refChars);
            refChars.rewind();

            // Stateful b2c files have leading and trailing
            // shift bytes for each mapping. However when
            // block encoded the output will consist of a single
            // leadByte followed by the raw DBCS byte values and
            // a final trail byte. The state variable shiftHackDBCS
            // which is true for stateful DBCS encodings is used
            // to conditionally strip away per-mapping shift bytes
            // from the comparison of expected versus actual encoded
            // byte values. This hack can be eliminated in Mustang
            // when sun.io converters and their associated tests are
            // removed.

            boolean boundaryBytes = false;
            int bytesPC = bytesPerChar;

            if (shiftHackDBCS && bytesPerChar==4) {
                bytesPC = 2;
                boundaryBytes = true;
                if ((out.get()) != (byte)0x0e) {
                    log.println("Missing lead byte");
                    return(false);
                }
            }

            byte[] rbs = new byte[bytesPC];
            byte[] obs = new byte[bytesPC];
            int e = 0;
            while (refChars.hasRemaining()) {
                int cp = getCodePoint(refChars);
                // Skip lead shift ref byte for stateful encoding tests
                if (shiftHackDBCS && bytesPC == 2)
                   refBytes.get();
                refBytes.get(rbs);
                out.get(obs);
                boolean eq = true;
                for (int i = 0; i < bytesPC; i++)
                    eq &= rbs[i] == obs[i];
                if (!eq) {
                    log.println("      Error: "
                                + printCodePoint(cp)
                                + " --> "
                                + printBytes(obs)
                                + ", expected "
                                + printBytes(rbs));
                    if (++e >= MAXERRS) {
                        log.println("      Too many errors, giving up");
                        break;
                    }
                }
                if (verbose) {
                    log.println("      "
                                + printCodePoint(cp)
                                + " --> "
                                + printBytes(rbs));
                }

                // For stateful encodings ignore/exclude per-mapping
                // trail bytes from byte comparison
                if (shiftHackDBCS && bytesPC == 2)
                   refBytes.get();
            }

            if (shiftHackDBCS && boundaryBytes) {
                if ((out.get()) != (byte)0x0f) {
                    log.println("Missing trail byte");
                    return(false);
                }
            }

            if (e == 0 && (refBytes.hasRemaining() || out.hasRemaining())) {
                // Paranoia: Didn't consume everything
                throw new IllegalStateException();
            }

            refBytes.rewind();
            refChars.rewind();
            return (e == 0);
        }

        private boolean run(int mode) throws Exception {
            log.println("  " + bytesPerChar
                        + " byte" + plural(bytesPerChar) + "/char");

            if (dRefBytes.capacity() < refBytes.capacity()) {
                dRefBytes = ByteBuffer.allocateDirect(refBytes.capacity());
            }
            if (dRefChars.capacity() < refChars.capacity()) {
                dRefChars = ByteBuffer.allocateDirect(refChars.capacity()*2)
                                      .asCharBuffer();
            }
            refBytes.flip();
            refChars.flip();
            dRefBytes.clear();
            dRefChars.clear();

            dRefBytes.put(refBytes).flip();
            dRefChars.put(refChars).flip();
            refBytes.flip();
            refChars.flip();

            boolean rv = true;
            if (mode != ENCODE) {
                rv &= decode(refBytes, refChars);
                rv &= decode(dRefBytes, dRefChars);
            }
            if (mode != DECODE) {
                rv &= encode(refBytes, refChars);
                rv &= encode(dRefBytes, dRefChars);
            }
            return rv;
        }

    }

    // Maximum bytes/char being tested
    private int maxBytesPerChar = 0;

    // Tests, indexed by bytesPerChar - 1
    private Test[] tests;

    private void clearTests() {
        maxBytesPerChar = 0;
        tests = new Test[0];
    }

    // Find the test for the given bytes/char value,
    // expanding the test array if needed
    //
    private Test testFor(int bpc) {
        if (bpc > maxBytesPerChar) {
            Test[] ts = new Test[bpc];
            System.arraycopy(tests, 0, ts, 0, maxBytesPerChar);
            for (int i = maxBytesPerChar; i < bpc; i++)
                ts[i] = new Test(i + 1);
            tests = ts;
            maxBytesPerChar = bpc;
        }
        return tests[bpc - 1];
    }

    // Compute the name of the test file for the given encoding and mode.  If
    // the file exists then return its name, otherwise return null.
    //
    private File testFile(String encoding, int mode) {
        File f = new File(testRootDir, encoding + extension[mode]);
        if (!f.exists())
            return null;
        return f;
    }

    // Parse the given b2c file and load up the required test objects
    //
    private void loadTests(File f)
        throws Exception
    {
        clearTests();
        FileInputStream in = new FileInputStream(f);
        try {
            Parser p = new Parser(in);
            Entry e = new Entry();

            while ((e = (Entry)p.next(e)) != null) {
                if (e.cp2 != 0)
                    continue;  // skip composite (base+cc) for now
                byte[] bs = e.bb;
                char[] cc = Character.toChars(e.cp);
                testFor(bs.length).put(bs, cc);
            }
            shiftHackDBCS = p.isStateful();
        } finally {
            in.close();
        }
    }

    private boolean run() throws Exception {
        encoder
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE)
            .onMalformedInput(CodingErrorAction.REPLACE);
        boolean rv = true;

        log.println();
        log.println(cs.name() + " (" + encoding + ")");

        // Outer loop runs three passes: roundtrip, irreversible encodings,
        // and then irreversible decodings
        for (int mode = ROUNDTRIP; mode <= DECODE; mode++) {
            File f = testFile(encoding, mode);
            if (f == null)
                continue;
            loadTests(f);
            for (int i = 0; i < maxBytesPerChar; i++)
                rv &= tests[i].run(mode);
        }
        return rv;
    }

    // For debugging: java CoderTest [-v] foo.b2c bar.b2c ...
    //
    public static void main(String args[])
        throws Exception
    {
        File d = new File(System.getProperty("test.src", "."));
        String[] av = (args.length != 0) ? args : d.list();
        int errors = 0;
        int tested = 0;
        int skipped = 0;

        for (int i = 0; i < av.length; i++) {
            String a = av[i];
            if (a.equals("-v")) {
                verbose = true;
                continue;
            }
            if (a.endsWith(".b2c")) {
                String encoding = a.substring(0, a.length() - 4);

                if (!Charset.isSupported(encoding)) {
                    log.println();
                    log.println("Not supported: " + encoding);
                    skipped++;
                    continue;
                }
                tested++;
                if (!new CoderTest(encoding).run())
                    errors++;
            }
        }

        log.println();
        log.println(tested + " charset" + plural(tested) + " tested, "
                    + skipped + " not supported");
        log.println();
        if (errors > 0)
            throw new Exception("Errors detected in "
                                + errors + " charset" + plural(errors));

    }
}
