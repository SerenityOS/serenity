/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6380723
 * @summary Decode many byte sequences in many ways (use -Dseed=X to set PRNG seed)
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main/timeout=1800 FindDecoderBugs
 * @author Martin Buchholz
 * @key randomness
 */

import java.util.*;
import java.util.regex.*;
import java.nio.*;
import java.nio.charset.*;
import jdk.test.lib.RandomFactory;

public class FindDecoderBugs {

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
            signature = hideBytes.matcher(signature).replaceAll("\"??\"");
            signature = hideChars.matcher(signature).replaceAll("\\u????");
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
        final byte[] ia;
        final char[] oa;
        final CoderResult cr;

        Result(ByteBuffer ib, CharBuffer ob, CoderResult cr) {
            ipos = ib.position();
            ia = toArray(ib);
            oa = toArray(ob);
            direct = ib.isDirect();
            limit = ob.limit();
            this.cr = cr;
        }

        static byte[] toArray(ByteBuffer b) {
            int pos = b.position();
            byte[] a = new byte[b.limit()];
            b.position(0);
            b.get(a);
            b.position(pos);
            return a;
        }

        static char[] toArray(CharBuffer b) {
            char[] a = new char[b.position()];
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

    // legend: r=regular d=direct In=Input Ou=Output
    static final int maxBufSize = 20;
    static final ByteBuffer[] ribs = new ByteBuffer[maxBufSize];
    static final ByteBuffer[] dibs = new ByteBuffer[maxBufSize];

    static final CharBuffer[] robs = new CharBuffer[maxBufSize];
    static final CharBuffer[] dobs = new CharBuffer[maxBufSize];
    static {
        for (int i = 0; i < maxBufSize; i++) {
            ribs[i] = ByteBuffer.allocate(i);
            dibs[i] = ByteBuffer.allocateDirect(i);
            robs[i] = CharBuffer.allocate(i);
            dobs[i] = ByteBuffer.allocateDirect(i*2).asCharBuffer();
        }
    }

    static class CharsetTester {
        private final Charset cs;
        private static final long maxFailures = 5;
        private long failures = 0;
        // private static final long maxCharsetFailures = Long.MAX_VALUE;
        private static final long maxCharsetFailures = 10000L;
        private final long failed0 = failed;

        CharsetTester(Charset cs) {
            this.cs = cs;
        }

        static boolean bug(String format, Object... args) {
            return reporter.bug(format, args);
        }

        Result recode(ByteBuffer ib, CharBuffer ob) {
            try {
                char canary = '\u4242';
                ib.clear();     // Prepare to read
                ob.clear();     // Prepare to write
                for (int i = 0; i < ob.limit(); i++)
                    ob.put(i, canary);
                CharsetDecoder coder = cs.newDecoder();
                CoderResult cr = coder.decode(ib, ob, false);
                equal(ib.limit(), ib.capacity());
                equal(ob.limit(), ob.capacity());
                Result r = new Result(ib, ob, cr);
                if (cr.isError())
                    check(cr.length() > 0);
                if (cr.isOverflow() && ob.remaining() > 10)
                    bug("OVERFLOW, but there's lots of room: %s %s",
                        cs, r);
//              if (cr.isOverflow() && ib.remaining() == 0)
//                  bug("OVERFLOW, yet remaining() == 0: %s %s",
//                      cs, r);
                if (cr.isError() && ib.remaining() < cr.length())
                    bug("remaining() < CoderResult.length(): %s %s",
                        cs, r);
//              if (ib.position() == 0 && ob.position() > 0)
//                  reporter. bug("output only if input consumed: %s %s",
//                                cs, r);
                // Should we warn if cr.isUnmappable() ??
                CoderResult cr2 = coder.decode(ib, ob, false);
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

        Result recode2(byte[] ia, int n) {
            int len = ia.length;
            ByteBuffer rib = ByteBuffer.wrap(ia);
            ByteBuffer dib = dibs[len];
            dib.clear(); dib.put(ia); dib.clear();
            CharBuffer rob = robs[n];
            CharBuffer dob = dobs[n];
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

        Result test(byte[] ia) {
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

        void testExhaustively(byte[] prefix, int n) {
            int len = prefix.length;
            byte[] ia = Arrays.copyOf(prefix, len + 1);
            for (int i = 0; i < 0x100; i++) {
                ia[len] = (byte) i;
                if (n == 1)
                    test(ia);
                else
                    testExhaustively(ia, n - 1);
            }
        }

        void testRandomly(byte[] prefix, int n) {
            int len = prefix.length;
            byte[] ia = Arrays.copyOf(prefix, len + n);
            for (int i = 0; i < 5000; i++) {
                for (int j = 0; j < n; j++)
                    ia[len + j] = randomByte();
                test(ia);
            }
        }

        void testPrefix(byte[] prefix) {
            if (prefix.length > 0)
                System.out.printf("Testing prefix %s%n", string(prefix));

            test(prefix);

            testExhaustively(prefix, 1);
            testExhaustively(prefix, 2);
            // Can you spare a week of CPU time?
            // testExhaustively(cs, tester, prefix, 3);

            testRandomly(prefix, 3);
            testRandomly(prefix, 4);
        }
    }

    private final static Random rnd = RandomFactory.getRandom();
    private static byte randomByte() {
        return (byte) rnd.nextInt(0x100);
    }
    private static byte[] randomBytes(int len) {
        byte[] a = new byte[len];
        for (int i = 0; i < len; i++)
            a[i] = randomByte();
        return a;
    }

    private static final byte SS2 = (byte) 0x8e;
    private static final byte SS3 = (byte) 0x8f;
    private static final byte ESC = (byte) 0x1b;
    private static final byte SO  = (byte) 0x0e;
    private static final byte SI  = (byte) 0x0f;

    private final static byte[][] stateChangers = {
        {SS2}, {SS3}, {SO}, {SI}
    };

    private final static byte[][]escapeSequences = {
        {ESC, '(', 'B'},
        {ESC, '(', 'I'},
        {ESC, '(', 'J'},
        {ESC, '$', '@'},
        {ESC, '$', 'A'},
        {ESC, '$', ')', 'A'},
        {ESC, '$', ')', 'C'},
        {ESC, '$', ')', 'G'},
        {ESC, '$', '*', 'H'},
        {ESC, '$', '+', 'I'},
        {ESC, '$', 'B'},
        {ESC, 'N'},
        {ESC, 'O'},
        {ESC, '$', '(', 'D'},
    };

    private static boolean isStateChanger(Charset cs, byte[] ia) {
        Result r = new CharsetTester(cs).recode2(ia, 9);
        return r == null ? false :
            (r.cr.isUnderflow() &&
             r.ipos == ia.length &&
             r.oa.length == 0);
    }

    private final static byte[][] incompletePrefixes = {
        {ESC},
        {ESC, '('},
        {ESC, '$'},
        {ESC, '$', '(',},
    };

    private static boolean isIncompletePrefix(Charset cs, byte[] ia) {
        Result r = new CharsetTester(cs).recode2(ia, 9);
        return r == null ? false :
            (r.cr.isUnderflow() &&
             r.ipos == 0 &&
             r.oa.length == 0);
    }

    private static void testCharset(Charset cs) throws Throwable {
        final String csn = cs.name();

        if (isBroken(csn)) {
            System.out.printf("Skipping possibly broken charset %s%n", csn);
            return;
        }
        System.out.println(csn);
        CharsetTester tester = new CharsetTester(cs);

        tester.testPrefix(new byte[0]);

        if (! csn.matches("(?:x-)?(?:UTF|JIS(?:_X)?0).*")) {
            for (byte[] prefix : stateChangers)
                if (isStateChanger(cs, prefix))
                    tester.testPrefix(prefix);

            for (byte[] prefix : incompletePrefixes)
                if (isIncompletePrefix(cs, prefix))
                    tester.testPrefix(prefix);

            if (isIncompletePrefix(cs, new byte[] {ESC}))
                for (byte[] prefix : escapeSequences)
                    if (isStateChanger(cs, prefix))
                        tester.testPrefix(prefix);
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
    static void equal(int x, int y) {
        if (x == y) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
