/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6233345 6381699 6381702 6381705 6381706
 * @summary Encode many char sequences in many ways
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main/timeout=1200 FindEncoderBugs
 * @author Martin Buchholz
 * @key randomness
 */

import java.util.*;
import java.util.regex.*;
import java.nio.*;
import java.nio.charset.*;
import jdk.test.lib.RandomFactory;

public class FindEncoderBugs {

    static boolean isBroken(String csn) {
        if (csn.equals("x-COMPOUND_TEXT")) return true;
        return false;
    }

    static <T extends Comparable<? super T>> List<T> sort(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        Collections.sort(list);
        return list;
    }

    static class TooManyFailures extends RuntimeException {
        private static final long serialVersionUID = 0L;
    }

    static String string(byte[] a) {
        final StringBuilder sb = new StringBuilder();
        for (byte b : a) {
            if (sb.length() != 0) sb.append(' ');
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    static String string(char[] a) {
        final StringBuilder sb = new StringBuilder();
        for (char c : a) {
            if (sb.length() != 0) sb.append(' ');
            sb.append(String.format("\\u%04x", (int) c));
        }
        return sb.toString();
    }

    static class Reporter {
        // Some machinery to make sure only a small number of errors
        // that are "too similar" are reported.
        static class Counts extends HashMap<String, Long> {
            private static final long serialVersionUID = -1;
            long inc(String signature) {
                Long count = get(signature);
                if (count == null) count = 0L;
                put(signature, count+1);
                return count+1;
            }
        }

        final Counts failureCounts = new Counts();
        final static long maxFailures = 2;

        final static Pattern hideBytes = Pattern.compile("\"[0-9a-f ]+\"");
        final static Pattern hideChars = Pattern.compile("\\\\u[0-9a-f]{4}");

        boolean bug(String format, Object... args) {
            String signature = String.format(format, args);
            //      signature = hideBytes.matcher(signature).replaceAll("\"??\"");
            //      signature = hideChars.matcher(signature).replaceAll("\\u????");
            failed++;
            if (failureCounts.inc(signature) <= maxFailures) {
                System.out.printf(format, args);
                System.out.println();
                return true;
            }
            return false;
        }

        void summarize() {
            for (String key : sort(failureCounts.keySet()))
                System.out.printf("-----%n%s%nfailures=%d%n",
                                  key, failureCounts.get(key));
        }
    }

    static final Reporter reporter = new Reporter();

    static class Result {
        final int limit;
        final int ipos;
        final boolean direct;
        final char[] ia;
        final byte[] oa;
        final CoderResult cr;

        private static byte[] toByteArray(ByteBuffer bb) {
            byte[] bytes = new byte[bb.position()];
            for (int i = 0; i < bytes.length; i++)
                bytes[i] = bb.get(i);
            return bytes;
        }

        Result(CharBuffer ib, ByteBuffer ob, CoderResult cr) {
            ipos = ib.position();
            ia = toArray(ib);
            oa = toArray(ob);
            direct = ib.isDirect();
            limit = ob.limit();
            this.cr = cr;
        }

        static char[] toArray(CharBuffer b) {
            int pos = b.position();
            char[] a = new char[b.limit()];
            b.position(0);
            b.get(a);
            b.position(pos);
            return a;
        }

        static byte[] toArray(ByteBuffer b) {
            byte[] a = new byte[b.position()];
            b.position(0);
            b.get(a);
            return a;
        }

        static boolean eq(Result x, Result y) {
            return x == y ||
                (x != null && y != null &&
                 (Arrays.equals(x.oa, y.oa) &&
                  x.ipos == y.ipos &&
                  x.cr == y.cr));
        }

        public String toString() {
            return String.format("\"%s\"[%d/%d] => %s \"%s\"[%d/%d]%s",
                                 string(ia), ipos, ia.length,
                                 cr, string(oa), oa.length, limit,
                                 (direct ? " (direct)" : ""));
        }
    }

    static class CharsetTester {
        private final Charset cs;
        private final boolean hasBom;
        private static final int maxFailures = 5;
        private int failures = 0;
        // private static final long maxCharsetFailures = Long.MAX_VALUE;
        private static final long maxCharsetFailures = 10000L;
        private final long failed0 = failed;

        // legend: r=regular d=direct In=Input Ou=Output
        static final int maxBufSize = 40;
        static final CharBuffer[] rInBuffers = new CharBuffer[maxBufSize];
        static final CharBuffer[] dInBuffers = new CharBuffer[maxBufSize];

        static final ByteBuffer[] rOuBuffers = new ByteBuffer[maxBufSize];
        static final ByteBuffer[] dOuBuffers = new ByteBuffer[maxBufSize];
        static {
            for (int i = 0; i < maxBufSize; i++) {
                rInBuffers[i] = CharBuffer.allocate(i);
                dInBuffers[i] = ByteBuffer.allocateDirect(i*2).asCharBuffer();
                rOuBuffers[i] = ByteBuffer.allocate(i);
                dOuBuffers[i] = ByteBuffer.allocateDirect(i);
            }
        }

        CharsetTester(Charset cs) {
            this.cs = cs;
            this.hasBom =
                cs.name().matches(".*BOM.*") ||
                cs.name().equals("UTF-16");
        }

        static boolean bug(String format, Object... args) {
            return reporter.bug(format, args);
        }

        static boolean hasBom(byte[] a) {
            switch (a.length) {
            case 2: case 4:
                int sum = 0;
                for (byte x : a)
                    sum += x;
                return sum == (byte) 0xfe + (byte) 0xff;
            default: return false;
            }
        }

        void testSurrogates() {
            int failures = 0;
            for (int i = 0; i < 10; i++) {
                Result r = test(new char[] { randomHighSurrogate() });
                if (r == null) break;
                if (! (r.cr.isUnderflow() &&
                       r.ipos == 0))
                    bug("Lone high surrogate not UNDERFLOW: %s %s",
                        cs, r);
            }
            for (int i = 0; i < 10; i++) {
                Result r = test(new char[] { randomLowSurrogate() });
                if (r == null) break;
                if (! (r.cr.isMalformed() && r.cr.length() == 1))
                    bug("Lone low surrogate not MALFORMED[1]: %s %s",
                        cs, r);
            }
            char[] chars = new char[2];
            for (int i = 0; i < 10; i++) {
                chars[0] = randomLowSurrogate(); // Always illegal
                chars[1] = randomChar();
                Result r = test(chars);
                if (r == null) break;
                if (! (r.cr.isMalformed() &&
                       r.cr.length() == 1 &&
                       (r.ipos == 0 || (hasBom && hasBom(r.oa))))) {
                    if (failures++ > 5) return;
                    bug("Unpaired low surrogate not MALFORMED[1]: %s %s",
                        cs, r);
                }
            }
            for (int i = 0; i < 10; i++) {
                chars[0] = randomHighSurrogate();
                do {
                    chars[1] = randomChar();
                } while (Character.isLowSurrogate(chars[1]));
                Result r = test(chars);
                if (r == null) break;
                if (! (r.cr.isMalformed() &&
                       r.cr.length() == 1 &&
                       (r.ipos == 0 || (hasBom && hasBom(r.oa))))) {
                    if (failures++ > 5) return;
                    bug("Unpaired high surrogate not MALFORMED[1]: %s %s",
                        cs, r);
                }
            }
            for (int i = 0; i < 1000; i++) {
                chars[0] = randomHighSurrogate();
                chars[1] = randomLowSurrogate();
                Result r = test(chars);
                if (r == null) break;
                if (! ((r.cr.isUnmappable() &&
                        r.cr.length() == 2 &&
                        r.oa.length == 0)
                       ||
                       (r.cr.isUnderflow() &&
                        r.oa.length > 0 &&
                        r.ipos == 2))) {
                    if (failures++ > 5) return;
                    bug("Legal supplementary character bug: %s %s",
                        cs, r);
                }
            }
        }

//              if (! (r.cr.isMalformed() &&
//                     r.cr.length() == 1 &&
//                     (rob.position() == 0 || hasBom(rob)))) {
//                  if (failures++ > 5) return;
//                  bug("Unpaired surrogate not malformed: %s %s",
//                               cs, r);
//              }
//          }

//                  dib.clear(); dib.put(chars); dib.flip();
//                  rib.position(0);
//                  rob.clear(); rob.limit(lim);
//                  for (CharBuffer ib : new CharBuffer[] { rib, dib }) {
//                      Result r = recode(ib, rob);
//                      if (! (r.cr.isMalformed() &&
//                             r.cr.length() == 1 &&
//                             (rob.position() == 0 || hasBom(rob)))) {
//                          if (failures++ > 5) return;
//                          bug("Unpaired surrogate not malformed: %s %s",
//                                       cs, r);
//                      }
//                  }
//                  //}
//              for (int i = 0; i < 10000; i++) {
//                  chars[0] = randomHighSurrogate();
//                  chars[1] = randomLowSurrogate();
//                  dib.clear(); dib.put(chars); dib.flip();
//                  rib.position(0);
//                  rob.clear(); rob.limit(lim);
//                  for (CharBuffer ib : new CharBuffer[] { rib, dib }) {
//                      Result r = recode(ib, rob);
//                      if (! ((r.cr.isUnmappable() &&
//                              r.cr.length() == 2 &&
//                              rob.position() == 0)
//                             ||
//                             (r.cr.isUnderflow() &&
//                              rob.position() > 0 &&
//                              ib.position() == 2))) {
//                          if (failures++ > 5) return;
//                          bug("Legal supplementary character bug: %s %s",
//                                       cs, r);
//                      }
//                  }
//              }
//          }
//      }

        Result recode(CharBuffer ib, ByteBuffer ob) {
            try {
                byte canary = 22;
                ib.clear();     // Prepare to read
                ob.clear();     // Prepare to write
                for (int i = 0; i < ob.limit(); i++)
                    ob.put(i, canary);
                CharsetEncoder coder = cs.newEncoder();
                CoderResult cr = coder.encode(ib, ob, false);
                equal(ib.limit(), ib.capacity());
                equal(ob.limit(), ob.capacity());
                Result r = new Result(ib, ob, cr);
                if (cr.isError())
                    check(cr.length() > 0);
                if (cr.isOverflow() && ob.remaining() > 10)
                    bug("OVERFLOW, but there's lots of room: %s %s",
                        cs, r);
//              if (cr.isOverflow() && ib.remaining() == 0 && ! hasBom)
//                  bug("OVERFLOW, yet remaining() == 0: %s %s",
//                      cs, r);
                if (cr.isError() && ib.remaining() < cr.length())
                    bug("remaining() < CoderResult.length(): %s %s",
                        cs, r);
//              if (ib.position() == 0
//                  && ob.position() > 0
//                  && ! hasBom(r.oa))
//                  bug("output only if input consumed: %s %s",
//                       cs, r);
                CoderResult cr2 = coder.encode(ib, ob, false);
                if (ib.position() != r.ipos ||
                    ob.position() != r.oa.length ||
                    cr != cr2)
                    bug("Coding operation not idempotent: %s%n    %s%n    %s",
                        cs, r, new Result(ib, ob, cr2));
                if (ob.position() < ob.limit() &&
                    ob.get(ob.position()) != canary)
                    bug("Buffer overrun: %s %s %s",
                        cs, r, ob.get(ob.position()));
                return r;
            } catch (Throwable t) {
                if (bug("Unexpected exception: %s %s %s",
                        cs, t.getClass().getSimpleName(),
                        new Result(ib, ob, null)))
                    t.printStackTrace();
                return null;
            }
        }

        Result recode2(char[] ia, int n) {
            int len = ia.length;
            CharBuffer rib = CharBuffer.wrap(ia);
            CharBuffer dib = dInBuffers[len];
            dib.clear(); dib.put(ia); dib.clear();
            ByteBuffer rob = rOuBuffers[n];
            ByteBuffer dob = dOuBuffers[n];
            equal(rob.limit(), n);
            equal(dob.limit(), n);
            check(dib.isDirect());
            check(dob.isDirect());
            Result r1 = recode(rib, rob);
            Result r2 = recode(dib, dob);
            if (r1 != null && r2 != null && ! Result.eq(r1, r2))
                bug("Results differ for direct buffers: %s%n    %s%n    %s",
                    cs, r1, r2);
            return r1;
        }

        Result test(char[] ia) {
            if (failed - failed0 >= maxCharsetFailures)
                throw new TooManyFailures();

            Result roomy = recode2(ia, maxBufSize - 1);
            if (roomy == null) return roomy;
            int olen = roomy.oa.length;
            if (olen > 0) {
                if (roomy.ipos == roomy.ia.length) {
                    Result perfectFit = recode2(ia, olen);
                    if (! Result.eq(roomy, perfectFit))
                        bug("Results differ: %s%n    %s%n    %s",
                            cs, roomy, perfectFit);
                }
                for (int i = 0; i < olen; i++) {
                    Result claustrophobic = recode2(ia, i);
                    if (claustrophobic == null) return roomy;
                    if (roomy.cr.isUnderflow() &&
                        ! claustrophobic.cr.isOverflow())
                        bug("Expected OVERFLOW: %s%n    %s%n    %s",
                            cs, roomy, claustrophobic);
                }
            }
            return roomy;
        }

        void testExhaustively(char[] prefix, int n) {
            int len = prefix.length;
            char[] ia = Arrays.copyOf(prefix, len + 1);
            for (int i = 0; i < 0x10000; i++) {
                ia[len] = (char) i;
                if (n == 1)
                    test(ia);
                else
                    testExhaustively(ia, n - 1);
            }
        }

        void testRandomly(char[] prefix, int n) {
            int len = prefix.length;
            char[] ia = Arrays.copyOf(prefix, len + n);
            for (int i = 0; i < 10000; i++) {
                for (int j = 0; j < n; j++)
                    ia[len + j] = randomChar();
                test(ia);
            }
        }

        void testISO88591InvalidChar() {
            // Several architectures implement the ISO-8859-1 encoder as an
            // intrinsic where the vectorised assembly has separate cases
            // for different input sizes, so exhaustively test all sizes
            // from 0 to maxBufSize to ensure we get coverage

            for (int i = 0; i < CharsetTester.maxBufSize; i++) {
                char[] ia = new char[i];
                for (int j = 0; j < i; j++)
                    ia[j] = randomChar();

                test(ia);

                // Test break on unrepresentable character
                for (int j = 0; j < i; j++) {
                    char[] iaInvalid = ia.clone();
                    iaInvalid[j] = (char)(randomChar() | 0x100);
                    test(iaInvalid);
                }
            }
        }

        void testPrefix(char[] prefix) {
            if (prefix.length > 0)
                System.out.printf("Testing prefix %s%n", string(prefix));

            test(prefix);

            testExhaustively(prefix, 1);
            // Can you spare a year of CPU time?
            //testExhaustively(prefix, 2);

            testRandomly(prefix, 2);
            testRandomly(prefix, 3);
        }
    }

    private final static Random rnd = RandomFactory.getRandom();
    private static char randomChar() {
        return (char) rnd.nextInt(Character.MAX_VALUE);
    }
    private static char randomHighSurrogate() {
        return (char) (Character.MIN_HIGH_SURROGATE + rnd.nextInt(1024));
    }
    private static char randomLowSurrogate() {
        return (char) (Character.MIN_LOW_SURROGATE + rnd.nextInt(1024));
    }

    private static void testCharset(Charset cs) throws Throwable {
        if (! cs.canEncode())
            return;

        final String csn = cs.name();

        if (isBroken(csn)) {
            System.out.printf("Skipping possibly broken charset %s%n", csn);
            return;
        }
        System.out.println(csn);

        CharsetTester tester = new CharsetTester(cs);

        tester.testSurrogates();

        tester.testPrefix(new char[] {});

        if (csn.equals("x-ISCII91")) {
            System.out.println("More ISCII testing...");
            new CharsetTester(cs).testPrefix(new char[]{'\u094d'}); // Halant
            new CharsetTester(cs).testPrefix(new char[]{'\u093c'}); // Nukta
        } else if (csn.equals("ISO-8859-1")) {
            System.out.println("More ISO-8859-1 testing...");
            new CharsetTester(cs).testISO88591InvalidChar();
        }
    }

    private static void realMain(String[] args) {
        for (Charset cs : sort(Charset.availableCharsets().values())) {
            try {
                testCharset(cs);
            } catch (TooManyFailures e) {
                System.out.printf("Too many failures for %s%n", cs);
            } catch (Throwable t) {
                unexpected(t);
            }
        }
        reporter.summarize();
    }

    //--------------------- Infrastructure ---------------------------
    static volatile long passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String format, Object... args) {
        System.out.println(String.format(format, args)); failed++;}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
