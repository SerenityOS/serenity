/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4235519 8004212 8005394 8007298 8006295 8006315 8006530 8007379 8008925
 *      8014217 8025003 8026330 8028397 8129544 8165243 8176379 8222187
 * @summary tests java.util.Base64
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main TestBase64
 * @key randomness
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import jdk.test.lib.RandomFactory;

public class TestBase64 {

    private static final Random rnd = RandomFactory.getRandom();

    public static void main(String args[]) throws Throwable {
        int numRuns  = 10;
        int numBytes = 200;
        if (args.length > 1) {
            numRuns  = Integer.parseInt(args[0]);
            numBytes = Integer.parseInt(args[1]);
        }

        test(Base64.getEncoder(), Base64.getDecoder(), numRuns, numBytes);
        test(Base64.getUrlEncoder(), Base64.getUrlDecoder(), numRuns, numBytes);
        test(Base64.getMimeEncoder(), Base64.getMimeDecoder(), numRuns, numBytes);

        byte[] nl_1 = new byte[] {'\n'};
        byte[] nl_2 = new byte[] {'\n', '\r'};
        byte[] nl_3 = new byte[] {'\n', '\r', '\n'};
        for (int i = 0; i < 10; i++) {
            int len = rnd.nextInt(200) + 4;
            test(Base64.getMimeEncoder(len, nl_1),
                 Base64.getMimeDecoder(),
                 numRuns, numBytes);
            test(Base64.getMimeEncoder(len, nl_2),
                 Base64.getMimeDecoder(),
                 numRuns, numBytes);
            test(Base64.getMimeEncoder(len, nl_3),
                 Base64.getMimeDecoder(),
                 numRuns, numBytes);
        }

        // test mime case with < 4 length
        for (int len = 0; len < 4; len++) {
            test(Base64.getMimeEncoder(len, nl_1),
                 Base64.getMimeDecoder(),
                 numRuns, numBytes);

            test(Base64.getMimeEncoder(len, nl_2),
                 Base64.getMimeDecoder(),
                 numRuns, numBytes);

            test(Base64.getMimeEncoder(len, nl_3),
                 Base64.getMimeDecoder(),
                 numRuns, numBytes);
        }

        testNull(Base64.getEncoder());
        testNull(Base64.getUrlEncoder());
        testNull(Base64.getMimeEncoder());
        testNull(Base64.getMimeEncoder(10, new byte[]{'\n'}));
        testNull(Base64.getDecoder());
        testNull(Base64.getUrlDecoder());
        testNull(Base64.getMimeDecoder());
        checkNull(() -> Base64.getMimeEncoder(10, null));

        testIOE(Base64.getEncoder());
        testIOE(Base64.getUrlEncoder());
        testIOE(Base64.getMimeEncoder());
        testIOE(Base64.getMimeEncoder(10, new byte[]{'\n'}));

        byte[] src = new byte[1024];
        rnd.nextBytes(src);
        final byte[] decoded = Base64.getEncoder().encode(src);
        testIOE(Base64.getDecoder(), decoded);
        testIOE(Base64.getMimeDecoder(), decoded);
        testIOE(Base64.getUrlDecoder(), Base64.getUrlEncoder().encode(src));

        // illegal line separator
        checkIAE(() -> Base64.getMimeEncoder(10, new byte[]{'\r', 'N'}));

        // malformed padding/ending
        testMalformedPadding();

        // illegal base64 character
        decoded[2] = (byte)0xe0;
        checkIAE(() -> Base64.getDecoder().decode(decoded));
        checkIAE(() -> Base64.getDecoder().decode(decoded, new byte[1024]));
        checkIAE(() -> Base64.getDecoder().decode(ByteBuffer.wrap(decoded)));

        // test single-non-base64 character for mime decoding
        testSingleNonBase64MimeDec();

        // test decoding of unpadded data
        testDecodeUnpadded();

        // test mime decoding with ignored character after padding
        testDecodeIgnoredAfterPadding();

        // given invalid args, encoder should not produce output
        testEncoderKeepsSilence(Base64.getEncoder());
        testEncoderKeepsSilence(Base64.getUrlEncoder());
        testEncoderKeepsSilence(Base64.getMimeEncoder());

        // given invalid args, decoder should not consume input
        testDecoderKeepsAbstinence(Base64.getDecoder());
        testDecoderKeepsAbstinence(Base64.getUrlDecoder());
        testDecoderKeepsAbstinence(Base64.getMimeDecoder());

        // tests patch addressing JDK-8222187
        // https://bugs.openjdk.java.net/browse/JDK-8222187
        testJDK_8222187();
    }

    private static void test(Base64.Encoder enc, Base64.Decoder dec,
                             int numRuns, int numBytes) throws Throwable {
        enc.encode(new byte[0]);
        dec.decode(new byte[0]);

        for (boolean withoutPadding : new boolean[] { false, true}) {
            if (withoutPadding) {
                 enc = enc.withoutPadding();
            }
            for (int i=0; i<numRuns; i++) {
                for (int j=1; j<numBytes; j++) {
                    byte[] orig = new byte[j];
                    rnd.nextBytes(orig);

                    // --------testing encode/decode(byte[])--------
                    byte[] encoded = enc.encode(orig);
                    byte[] decoded = dec.decode(encoded);

                    checkEqual(orig, decoded,
                               "Base64 array encoding/decoding failed!");
                    if (withoutPadding) {
                        if (encoded[encoded.length - 1] == '=')
                            throw new RuntimeException(
                               "Base64 enc.encode().withoutPadding() has padding!");
                    }
                    // --------testing encodetoString(byte[])/decode(String)--------
                    String str = enc.encodeToString(orig);
                    if (!Arrays.equals(str.getBytes("ASCII"), encoded)) {
                        throw new RuntimeException(
                            "Base64 encodingToString() failed!");
                    }
                    byte[] buf = dec.decode(new String(encoded, "ASCII"));
                    checkEqual(buf, orig, "Base64 decoding(String) failed!");

                    //-------- testing encode/decode(Buffer)--------
                    testEncode(enc, ByteBuffer.wrap(orig), encoded);
                    ByteBuffer bin = ByteBuffer.allocateDirect(orig.length);
                    bin.put(orig).flip();
                    testEncode(enc, bin, encoded);

                    testDecode(dec, ByteBuffer.wrap(encoded), orig);
                    bin = ByteBuffer.allocateDirect(encoded.length);
                    bin.put(encoded).flip();
                    testDecode(dec, bin, orig);

                    // --------testing decode.wrap(input stream)--------
                    // 1) random buf length
                    ByteArrayInputStream bais = new ByteArrayInputStream(encoded);
                    InputStream is = dec.wrap(bais);
                    buf = new byte[orig.length + 10];
                    int len = orig.length;
                    int off = 0;
                    while (true) {
                        int n = rnd.nextInt(len);
                        if (n == 0)
                            n = 1;
                        n = is.read(buf, off, n);
                        if (n == -1) {
                            checkEqual(off, orig.length,
                                       "Base64 stream decoding failed");
                            break;
                        }
                        off += n;
                        len -= n;
                        if (len == 0)
                            break;
                    }
                    buf = Arrays.copyOf(buf, off);
                    checkEqual(buf, orig, "Base64 stream decoding failed!");

                    // 2) read one byte each
                    bais.reset();
                    is = dec.wrap(bais);
                    buf = new byte[orig.length + 10];
                    off = 0;
                    int b;
                    while ((b = is.read()) != -1) {
                        buf[off++] = (byte)b;
                    }
                    buf = Arrays.copyOf(buf, off);
                    checkEqual(buf, orig, "Base64 stream decoding failed!");

                    // --------testing encode.wrap(output stream)--------
                    ByteArrayOutputStream baos = new ByteArrayOutputStream((orig.length + 2) / 3 * 4 + 10);
                    OutputStream os = enc.wrap(baos);
                    off = 0;
                    len = orig.length;
                    for (int k = 0; k < 5; k++) {
                        if (len == 0)
                            break;
                        int n = rnd.nextInt(len);
                        if (n == 0)
                            n = 1;
                        os.write(orig, off, n);
                        off += n;
                        len -= n;
                    }
                    if (len != 0)
                        os.write(orig, off, len);
                    os.close();
                    buf = baos.toByteArray();
                    checkEqual(buf, encoded, "Base64 stream encoding failed!");

                    // 2) write one byte each
                    baos.reset();
                    os = enc.wrap(baos);
                    off = 0;
                    while (off < orig.length) {
                        os.write(orig[off++]);
                    }
                    os.close();
                    buf = baos.toByteArray();
                    checkEqual(buf, encoded, "Base64 stream encoding failed!");

                    // --------testing encode(in, out); -> bigger buf--------
                    buf = new byte[encoded.length + rnd.nextInt(100)];
                    int ret = enc.encode(orig, buf);
                    checkEqual(ret, encoded.length,
                               "Base64 enc.encode(src, null) returns wrong size!");
                    buf = Arrays.copyOf(buf, ret);
                    checkEqual(buf, encoded,
                               "Base64 enc.encode(src, dst) failed!");

                    // --------testing decode(in, out); -> bigger buf--------
                    buf = new byte[orig.length + rnd.nextInt(100)];
                    ret = dec.decode(encoded, buf);
                    checkEqual(ret, orig.length,
                              "Base64 enc.encode(src, null) returns wrong size!");
                    buf = Arrays.copyOf(buf, ret);
                    checkEqual(buf, orig,
                               "Base64 dec.decode(src, dst) failed!");

                }
            }
        }
    }

    private static final byte[] ba_null = null;
    private static final String str_null = null;
    private static final ByteBuffer bb_null = null;

    private static void testNull(Base64.Encoder enc) {
        checkNull(() -> enc.encode(ba_null));
        checkNull(() -> enc.encodeToString(ba_null));
        checkNull(() -> enc.encode(ba_null, new byte[10]));
        checkNull(() -> enc.encode(new byte[10], ba_null));
        checkNull(() -> enc.encode(bb_null));
        checkNull(() -> enc.wrap((OutputStream)null));
    }

    private static void testNull(Base64.Decoder dec) {
        checkNull(() -> dec.decode(ba_null));
        checkNull(() -> dec.decode(str_null));
        checkNull(() -> dec.decode(ba_null, new byte[10]));
        checkNull(() -> dec.decode(new byte[10], ba_null));
        checkNull(() -> dec.decode(bb_null));
        checkNull(() -> dec.wrap((InputStream)null));
    }

    @FunctionalInterface
    private static interface Testable {
        public void test() throws Throwable;
    }

    private static void testIOE(Base64.Encoder enc) throws Throwable {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        OutputStream os = enc.wrap(baos);
        os.write(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9});
        os.close();
        checkIOE(() -> os.write(10));
        checkIOE(() -> os.write(new byte[] {10}));
        checkIOE(() -> os.write(new byte[] {10}, 1, 4));
    }

    private static void testIOE(Base64.Decoder dec, byte[] decoded) throws Throwable {
        ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
        InputStream is = dec.wrap(bais);
        is.read(new byte[10]);
        is.close();
        checkIOE(() -> is.read());
        checkIOE(() -> is.read(new byte[] {10}));
        checkIOE(() -> is.read(new byte[] {10}, 1, 4));
        checkIOE(() -> is.available());
        checkIOE(() -> is.skip(20));
    }

    private static final void checkNull(Runnable r) {
        try {
            r.run();
            throw new RuntimeException("NPE is not thrown as expected");
        } catch (NullPointerException npe) {}
    }

    private static final void checkIOE(Testable t) throws Throwable {
        try {
            t.test();
            throw new RuntimeException("IOE is not thrown as expected");
        } catch (IOException ioe) {}
    }

    private static final void checkIAE(Runnable r) throws Throwable {
        try {
            r.run();
            throw new RuntimeException("IAE is not thrown as expected");
        } catch (IllegalArgumentException iae) {}
    }

    private static void testDecodeIgnoredAfterPadding() throws Throwable {
        for (byte nonBase64 : new byte[] {'#', '(', '!', '\\', '-', '_', '\n', '\r'}) {
            byte[][] src = new byte[][] {
                "A".getBytes("ascii"),
                "AB".getBytes("ascii"),
                "ABC".getBytes("ascii"),
                "ABCD".getBytes("ascii"),
                "ABCDE".getBytes("ascii")
            };
            Base64.Encoder encM = Base64.getMimeEncoder();
            Base64.Decoder decM = Base64.getMimeDecoder();
            Base64.Encoder enc = Base64.getEncoder();
            Base64.Decoder dec = Base64.getDecoder();
            for (int i = 0; i < src.length; i++) {
                // decode(byte[])
                byte[] encoded = encM.encode(src[i]);
                encoded = Arrays.copyOf(encoded, encoded.length + 1);
                encoded[encoded.length - 1] = nonBase64;
                checkEqual(decM.decode(encoded), src[i], "Non-base64 char is not ignored");
                byte[] decoded = new byte[src[i].length];
                decM.decode(encoded, decoded);
                checkEqual(decoded, src[i], "Non-base64 char is not ignored");

                try {
                    dec.decode(encoded);
                    throw new RuntimeException("No IAE for non-base64 char");
                } catch (IllegalArgumentException iae) {}
            }
        }
    }

    private static void testMalformedPadding() throws Throwable {
        Object[] data = new Object[] {
            "$=#",       "",      0,      // illegal ending unit
            "A",         "",      0,      // dangling single byte
            "A=",        "",      0,
            "A==",       "",      0,
            "QUJDA",     "ABC",   4,
            "QUJDA=",    "ABC",   4,
            "QUJDA==",   "ABC",   4,

            "=",         "",      0,      // unnecessary padding
            "QUJD=",     "ABC",   4,      //"ABC".encode() -> "QUJD"

            "AA=",       "",      0,      // incomplete padding
            "QQ=",       "",      0,
            "QQ=N",      "",      0,      // incorrect padding
            "QQ=?",      "",      0,
            "QUJDQQ=",   "ABC",   4,
            "QUJDQQ=N",  "ABC",   4,
            "QUJDQQ=?",  "ABC",   4,
        };

        Base64.Decoder[] decs = new Base64.Decoder[] {
            Base64.getDecoder(),
            Base64.getUrlDecoder(),
            Base64.getMimeDecoder()
        };

        for (Base64.Decoder dec : decs) {
            for (int i = 0; i < data.length; i += 3) {
                final String srcStr = (String)data[i];
                final byte[] srcBytes = srcStr.getBytes("ASCII");
                final ByteBuffer srcBB = ByteBuffer.wrap(srcBytes);
                byte[] expected = ((String)data[i + 1]).getBytes("ASCII");
                int pos = (Integer)data[i + 2];

                // decode(byte[])
                checkIAE(() -> dec.decode(srcBytes));

                // decode(String)
                checkIAE(() -> dec.decode(srcStr));

                // decode(ByteBuffer)
                checkIAE(() -> dec.decode(srcBB));

                // wrap stream
                checkIOE(new Testable() {
                    public void test() throws IOException {
                        try (InputStream is = dec.wrap(new ByteArrayInputStream(srcBytes))) {
                            while (is.read() != -1);
                        }
                }});
            }
        }

        // anything left after padding is "invalid"/IAE, if
        // not MIME. In case of MIME, non-base64 character(s)
        // is ignored.
        checkIAE(() -> Base64.getDecoder().decode("AA==\u00D2"));
        checkIAE(() -> Base64.getUrlDecoder().decode("AA==\u00D2"));
        Base64.getMimeDecoder().decode("AA==\u00D2");
     }

    private static void  testDecodeUnpadded() throws Throwable {
        byte[] srcA = new byte[] { 'Q', 'Q' };
        byte[] srcAA = new byte[] { 'Q', 'Q', 'E'};
        Base64.Decoder dec = Base64.getDecoder();
        byte[] ret = dec.decode(srcA);
        if (ret[0] != 'A')
            throw new RuntimeException("Decoding unpadding input A failed");
        ret = dec.decode(srcAA);
        if (ret[0] != 'A' && ret[1] != 'A')
            throw new RuntimeException("Decoding unpadding input AA failed");
        ret = new byte[10];
        if (dec.wrap(new ByteArrayInputStream(srcA)).read(ret) != 1 &&
            ret[0] != 'A')
            throw new RuntimeException("Decoding unpadding input A from stream failed");
        if (dec.wrap(new ByteArrayInputStream(srcA)).read(ret) != 2 &&
            ret[0] != 'A' && ret[1] != 'A')
            throw new RuntimeException("Decoding unpadding input AA from stream failed");
    }

    // single-non-base64-char should be ignored for mime decoding, but
    // iae for basic decoding
    private static void testSingleNonBase64MimeDec() throws Throwable {
        for (String nonBase64 : new String[] {"#", "(", "!", "\\", "-", "_"}) {
            if (Base64.getMimeDecoder().decode(nonBase64).length != 0) {
                throw new RuntimeException("non-base64 char is not ignored");
            }
            try {
                Base64.getDecoder().decode(nonBase64);
                throw new RuntimeException("No IAE for single non-base64 char");
            } catch (IllegalArgumentException iae) {}
        }
    }

    private static final void testEncode(Base64.Encoder enc, ByteBuffer bin, byte[] expected)
        throws Throwable {

        ByteBuffer bout = enc.encode(bin);
        byte[] buf = new byte[bout.remaining()];
        bout.get(buf);
        if (bin.hasRemaining()) {
            throw new RuntimeException(
                "Base64 enc.encode(ByteBuffer) failed!");
        }
        checkEqual(buf, expected, "Base64 enc.encode(bf, bf) failed!");
    }

    private static final void testDecode(Base64.Decoder dec, ByteBuffer bin, byte[] expected)
        throws Throwable {

        ByteBuffer bout = dec.decode(bin);
        byte[] buf = new byte[bout.remaining()];
        bout.get(buf);
        checkEqual(buf, expected, "Base64 dec.decode(bf) failed!");
    }

    private static final void checkEqual(int v1, int v2, String msg)
        throws Throwable {
       if (v1 != v2) {
           System.out.printf("    v1=%d%n", v1);
           System.out.printf("    v2=%d%n", v2);
           throw new RuntimeException(msg);
       }
    }

    private static final void checkEqual(byte[] r1, byte[] r2, String msg)
        throws Throwable {
       if (!Arrays.equals(r1, r2)) {
           System.out.printf("    r1[%d]=[%s]%n", r1.length, new String(r1));
           System.out.printf("    r2[%d]=[%s]%n", r2.length, new String(r2));
           throw new RuntimeException(msg);
       }
    }

    // remove line feeds,
    private static final byte[] normalize(byte[] src) {
        int n = 0;
        boolean hasUrl = false;
        for (int i = 0; i < src.length; i++) {
            if (src[i] == '\r' || src[i] == '\n')
                n++;
            if (src[i] == '-' || src[i] == '_')
                hasUrl = true;
        }
        if (n == 0 && hasUrl == false)
            return src;
        byte[] ret = new byte[src.length - n];
        int j = 0;
        for (int i = 0; i < src.length; i++) {
            if (src[i] == '-')
                ret[j++] = '+';
            else if (src[i] == '_')
                ret[j++] = '/';
            else if (src[i] != '\r' && src[i] != '\n')
                ret[j++] = src[i];
        }
        return ret;
    }

    private static void testEncoderKeepsSilence(Base64.Encoder enc)
            throws Throwable {
        List<Integer> vals = new ArrayList<>(List.of(Integer.MIN_VALUE,
                Integer.MIN_VALUE + 1, -1111, -2, -1, 0, 1, 2, 3, 1111,
                Integer.MAX_VALUE - 1, Integer.MAX_VALUE));
        vals.addAll(List.of(rnd.nextInt(), rnd.nextInt(), rnd.nextInt(),
                rnd.nextInt()));
        byte[] buf = new byte[] {1, 0, 91};
        for (int off : vals) {
            for (int len : vals) {
                if (off >= 0 && len >= 0 && off <= buf.length - len) {
                    // valid args, skip them
                    continue;
                }
                // invalid args, test them
                System.out.println("testing off=" + off + ", len=" + len);

                ByteArrayOutputStream baos = new ByteArrayOutputStream(100);
                try (OutputStream os = enc.wrap(baos)) {
                    os.write(buf, off, len);
                    throw new RuntimeException("Expected IOOBEx was not thrown");
                } catch (IndexOutOfBoundsException expected) {
                }
                if (baos.size() > 0)
                    throw new RuntimeException("No output was expected, but got "
                            + baos.size() + " bytes");
            }
        }
    }

    private static void testDecoderKeepsAbstinence(Base64.Decoder dec)
            throws Throwable {
        List<Integer> vals = new ArrayList<>(List.of(Integer.MIN_VALUE,
                Integer.MIN_VALUE + 1, -1111, -2, -1, 0, 1, 2, 3, 1111,
                Integer.MAX_VALUE - 1, Integer.MAX_VALUE));
        vals.addAll(List.of(rnd.nextInt(), rnd.nextInt(), rnd.nextInt(),
                rnd.nextInt()));
        byte[] buf = new byte[3];
        for (int off : vals) {
            for (int len : vals) {
                if (off >= 0 && len >= 0 && off <= buf.length - len) {
                    // valid args, skip them
                    continue;
                }
                // invalid args, test them
                System.out.println("testing off=" + off + ", len=" + len);

                String input = "AAAAAAAAAAAAAAAAAAAAAA";
                ByteArrayInputStream bais =
                        new ByteArrayInputStream(input.getBytes("Latin1"));
                try (InputStream is = dec.wrap(bais)) {
                    is.read(buf, off, len);
                    throw new RuntimeException("Expected IOOBEx was not thrown");
                } catch (IndexOutOfBoundsException expected) {
                }
                if (bais.available() != input.length())
                    throw new RuntimeException("No input should be consumed, "
                            + "but consumed " + (input.length() - bais.available())
                            + " bytes");
            }
        }
    }

    private static void testJDK_8222187() throws Throwable {
        byte[] orig = "12345678".getBytes("US-ASCII");
        byte[] encoded = Base64.getEncoder().encode(orig);
        // decode using different buffer sizes, up to a longer one than needed
        for (int bufferSize = 1; bufferSize <= encoded.length + 1; bufferSize++) {
            try (
                    InputStream in = Base64.getDecoder().wrap(
                            new ByteArrayInputStream(encoded));
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ) {
                byte[] buffer = new byte[bufferSize];
                int read;
                while ((read = in.read(buffer, 0, bufferSize)) >= 0) {
                    baos.write(buffer, 0, read);
                }
                // compare result, output info if lengths do not match
                byte[] decoded = baos.toByteArray();
                checkEqual(decoded, orig, "Base64 stream decoding failed!");
            }
        }
    }
}
