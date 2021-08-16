/*
 * Copyright (c) 2015, Red Hat, Inc.
 * Copyright (c) 2015, Oracle, Inc.
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
 * @bug 8069072
 * @modules java.base/com.sun.crypto.provider:open
 * @summary Test vectors for com.sun.crypto.provider.GHASH.
 *
 * Single iteration to verify software-only GHASH algorithm.
 * @run main TestGHASH
 *
 * Multi-iteration to verify test intrinsics GHASH, if available.
 * Many iterations are needed so we are sure hotspot will use intrinsic
 * @run main TestGHASH -n 10000
 */
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class TestGHASH {

    private final Constructor<?> GHASH;
    private final Method UPDATE;
    private final Method DIGEST;

    TestGHASH(String className) throws Exception {
        Class<?> cls = Class.forName(className);
        GHASH = cls.getDeclaredConstructor(byte[].class);
        GHASH.setAccessible(true);
        UPDATE = cls.getDeclaredMethod("update", byte[].class);
        UPDATE.setAccessible(true);
        DIGEST = cls.getDeclaredMethod("digest");
        DIGEST.setAccessible(true);
    }


    private Object newGHASH(byte[] H) throws Exception {
        return GHASH.newInstance(H);
    }

    private void updateGHASH(Object hash, byte[] data)
            throws Exception {
        UPDATE.invoke(hash, data);
    }

    private byte[] digestGHASH(Object hash) throws Exception {
        return (byte[]) DIGEST.invoke(hash);
    }

    private static final String HEX_DIGITS = "0123456789abcdef";

    private static String hex(byte[] bs) {
        StringBuilder sb = new StringBuilder(2 * bs.length);
        for (byte b : bs) {
            sb.append(HEX_DIGITS.charAt((b >> 4) & 0xF));
            sb.append(HEX_DIGITS.charAt(b & 0xF));
        }
        return sb.toString();
    }

    private static byte[] bytes(String hex) {
        if ((hex.length() & 1) != 0) {
            throw new AssertionError();
        }
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; ++i) {
            int a = HEX_DIGITS.indexOf(hex.charAt(2 * i));
            int b = HEX_DIGITS.indexOf(hex.charAt(2 * i + 1));
            if ((a | b) < 0) {
                if (a < 0) {
                    throw new AssertionError(
                            "bad character " + (int) hex.charAt(2 * i));
                }
                throw new AssertionError(
                        "bad character " + (int) hex.charAt(2 * i + 1));
            }
            result[i] = (byte) ((a << 4) | b);
        }
        return result;
    }

    private static byte[] bytes(long L0, long L1) {
        return ByteBuffer.allocate(16)
                .putLong(L0)
                .putLong(L1)
                .array();
    }

    private void check(int testCase, String H, String A,
            String C, String expected) throws Exception {
        int lenA = A.length() * 4;
        while ((A.length() % 32) != 0) {
            A += '0';
        }
        int lenC = C.length() * 4;
        while ((C.length() % 32) != 0) {
            C += '0';
        }

        Object hash = newGHASH(bytes(H));
        updateGHASH(hash, bytes(A));
        updateGHASH(hash, bytes(C));
        updateGHASH(hash, bytes(lenA, lenC));
        byte[] digest = digestGHASH(hash);
        String actual = hex(digest);
        if (!expected.equals(actual)) {
            throw new AssertionError(String.format("%d: expected %s, got %s",
                    testCase, expected, actual));
        }
    }

    public static void main(String[] args) throws Exception {
        TestGHASH test;
        String test_class = "com.sun.crypto.provider.GHASH";
        int i = 0;
        int num_of_loops = 1;
        while (args.length > i) {
            if (args[i].compareTo("-c") == 0) {
                test_class = args[++i];
            } else if (args[i].compareTo("-n") == 0) {
                num_of_loops = Integer.parseInt(args[++i]);
            }
            i++;
        }

        System.out.println("Running " + num_of_loops + " iterations.");
        test = new TestGHASH(test_class);
        i = 0;

        while (num_of_loops > i) {
            // Test vectors from David A. McGrew, John Viega,
            // "The Galois/Counter Mode of Operation (GCM)", 2005.
            // <http://csrc.nist.gov/groups/ST/toolkit/BCM/documents/proposedmodes/gcm/gcm-revised-spec.pdf>
            test.check(1, "66e94bd4ef8a2c3b884cfa59ca342b2e", "", "",
                       "00000000000000000000000000000000");
            test.check(2,
                       "66e94bd4ef8a2c3b884cfa59ca342b2e", "",
                       "0388dace60b6a392f328c2b971b2fe78",
                       "f38cbb1ad69223dcc3457ae5b6b0f885");
            test.check(3,
                       "b83b533708bf535d0aa6e52980d53b78", "",
                       "42831ec2217774244b7221b784d0d49c" +
                       "e3aa212f2c02a4e035c17e2329aca12e" +
                       "21d514b25466931c7d8f6a5aac84aa05" +
                       "1ba30b396a0aac973d58e091473f5985",
                       "7f1b32b81b820d02614f8895ac1d4eac");
            test.check(4,
                       "b83b533708bf535d0aa6e52980d53b78",
                       "feedfacedeadbeeffeedfacedeadbeef" + "abaddad2",
                       "42831ec2217774244b7221b784d0d49c" +
                       "e3aa212f2c02a4e035c17e2329aca12e" +
                       "21d514b25466931c7d8f6a5aac84aa05" +
                       "1ba30b396a0aac973d58e091",
                       "698e57f70e6ecc7fd9463b7260a9ae5f");
            test.check(5, "b83b533708bf535d0aa6e52980d53b78",
                       "feedfacedeadbeeffeedfacedeadbeef" + "abaddad2",
                       "61353b4c2806934a777ff51fa22a4755" +
                       "699b2a714fcdc6f83766e5f97b6c7423" +
                       "73806900e49f24b22b097544d4896b42" +
                       "4989b5e1ebac0f07c23f4598",
                       "df586bb4c249b92cb6922877e444d37b");
            i++;
        }
    }
}
