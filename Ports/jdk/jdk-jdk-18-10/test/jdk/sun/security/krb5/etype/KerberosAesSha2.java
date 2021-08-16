/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8014628
 * @library /test/lib
 * @modules java.base/sun.security.util
 *          java.security.jgss/sun.security.krb5.internal.crypto.dk:+open
 * @summary https://tools.ietf.org/html/rfc8009 Test Vectors
 */

import javax.crypto.Cipher;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HexFormat;

import sun.security.krb5.internal.crypto.dk.AesSha2DkCrypto;
import jdk.test.lib.hexdump.HexPrinter;

public class KerberosAesSha2 {

    public static void main(String[] args) throws Exception {

        AesSha2DkCrypto dk128 = new AesSha2DkCrypto(128);
        AesSha2DkCrypto dk256 = new AesSha2DkCrypto(256);

        boolean aes256ok = Cipher.getMaxAllowedKeyLength("AES") >= 256;

        // Sample results for string-to-key conversion:
        char[] pass = "password".toCharArray();
        byte[] salt = cat(
                hex("10 DF 9D D7 83 E5 BC 8A CE A1 73 0E 74 35 5F 61"),
                "ATHENA.MIT.EDUraeburn".getBytes());

        check(stringToKey(dk128, pass, salt, null),
                hex("08 9B CA 48 B1 05 EA 6E A7 7C A5 D2 F3 9D C5 E7"));

        check(stringToKey(dk256, pass, salt, null),
                hex("45 BD 80 6D BF 6A 83 3A 9C FF C1 C9 45 89 A2 22\n" +
                    "36 7A 79 BC 21 C4 13 71 89 06 E9 F5 78 A7 84 67"));

        // Sample results for key derivation:
        byte[] bk16 = hex("37 05 D9 60 80 C1 77 28 A0 E8 00 EA B6 E0 D2 3C");

        check(deriveKey(dk128, bk16, 2, (byte) 0x99),
                hex("B3 1A 01 8A 48 F5 47 76 F4 03 E9 A3 96 32 5D C3"));
        check(deriveKey(dk128, bk16, 2, (byte) 0xaa),
                hex("9B 19 7D D1 E8 C5 60 9D 6E 67 C3 E3 7C 62 C7 2E"));
        check(deriveKey(dk128, bk16, 2, (byte) 0x55),
                hex("9F DA 0E 56 AB 2D 85 E1 56 9A 68 86 96 C2 6A 6C"));

        byte[] bk32 = hex(
                "6D 40 4D 37 FA F7 9F 9D F0 D3 35 68 D3 20 66 98\n" +
                "00 EB 48 36 47 2E A8 A0 26 D1 6B 71 82 46 0C 52");

        check(deriveKey(dk256, bk32, 2, (byte) 0x99), hex(
                "EF 57 18 BE 86 CC 84 96 3D 8B BB 50 31 E9 F5 C4\n" +
                "BA 41 F2 8F AF 69 E7 3D"));
        check(deriveKey(dk256, bk32, 2, (byte) 0xaa), hex(
                "56 AB 22 BE E6 3D 82 D7 BC 52 27 F6 77 3F 8E A7\n" +
                "A5 EB 1C 82 51 60 C3 83 12 98 0C 44 2E 5C 7E 49"));
        check(deriveKey(dk256, bk32, 2, (byte) 0x55), hex(
                "69 B1 65 14 E3 CD 8E 56 B8 20 10 D5 C7 30 12 B6\n" +
                "22 C4 D0 0F FC 23 ED 1F"));

        // Sample encryptions (all using the default cipher state):

        check(enc(dk128, hex("7E 58 95 EA F2 67 24 35 BA D8 17 F5 45 A3 71 48"),
                    bk16, hex("")),
                hex("EF 85 FB 89 0B B8 47 2F 4D AB 20 39 4D CA 78 1D\n" +
                    "AD 87 7E DA 39 D5 0C 87 0C 0D 5A 0A 8E 48 C7 18"));

        check(enc(dk128, hex("7B CA 28 5E 2F D4 13 0F B5 5B 1A 5C 83 BC 5B 24"),
                    bk16, hex("00 01 02 03 04 05")),
                hex("84 D7 F3 07 54 ED 98 7B AB 0B F3 50 6B EB 09 CF\n" +
                    "B5 54 02 CE F7 E6 87 7C E9 9E 24 7E 52 D1 6E D4\n" +
                    "42 1D FD F8 97 6C"));

        check(enc(dk128, hex("56 AB 21 71 3F F6 2C 0A 14 57 20 0F 6F A9 94 8F"),
                    bk16, hex("00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F")),
                hex("35 17 D6 40 F5 0D DC 8A D3 62 87 22 B3 56 9D 2A\n" +
                    "E0 74 93 FA 82 63 25 40 80 EA 65 C1 00 8E 8F C2\n" +
                    "95 FB 48 52 E7 D8 3E 1E 7C 48 C3 7E EB E6 B0 D3"));

        check(enc(dk128, hex("A7 A4 E2 9A 47 28 CE 10 66 4F B6 4E 49 AD 3F AC"),
                    bk16, hex("00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n" +
                              "10 11 12 13 14")),
                hex("72 0F 73 B1 8D 98 59 CD 6C CB 43 46 11 5C D3 36\n" +
                    "C7 0F 58 ED C0 C4 43 7C 55 73 54 4C 31 C8 13 BC\n" +
                    "E1 E6 D0 72 C1 86 B3 9A 41 3C 2F 92 CA 9B 83 34\n" +
                    "A2 87 FF CB FC\n"));

        if (aes256ok) {
            check(enc(dk256, hex("F7 64 E9 FA 15 C2 76 47 8B 2C 7D 0C 4E 5F 58 E4"),
                        bk32, hex("")),
                    hex("41 F5 3F A5 BF E7 02 6D 91 FA F9 BE 95 91 95 A0\n" +
                        "58 70 72 73 A9 6A 40 F0 A0 19 60 62 1A C6 12 74\n" +
                        "8B 9B BF BE 7E B4 CE 3C\n"));

            check(enc(dk256, hex("B8 0D 32 51 C1 F6 47 14 94 25 6F FE 71 2D 0B 9A"),
                        bk32, hex("00 01 02 03 04 05")),
                    hex("4E D7 B3 7C 2B CA C8 F7 4F 23 C1 CF 07 E6 2B C7\n" +
                        "B7 5F B3 F6 37 B9 F5 59 C7 F6 64 F6 9E AB 7B 60\n" +
                        "92 23 75 26 EA 0D 1F 61 CB 20 D6 9D 10 F2\n"));

            check(enc(dk256, hex("53 BF 8A 0D 10 52 65 D4 E2 76 42 86 24 CE 5E 63"),
                        bk32, hex("00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F")),
                    hex("BC 47 FF EC 79 98 EB 91 E8 11 5C F8 D1 9D AC 4B\n" +
                        "BB E2 E1 63 E8 7D D3 7F 49 BE CA 92 02 77 64 F6\n" +
                        "8C F5 1F 14 D7 98 C2 27 3F 35 DF 57 4D 1F 93 2E\n" +
                        "40 C4 FF 25 5B 36 A2 66\n"));

            check(enc(dk256, hex("76 3E 65 36 7E 86 4F 02 F5 51 53 C7 E3 B5 8A F1"),
                        bk32, hex("00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n" +
                                  "10 11 12 13 14")),
                    hex("40 01 3E 2D F5 8E 87 51 95 7D 28 78 BC D2 D6 FE\n" +
                        "10 1C CF D5 56 CB 1E AE 79 DB 3C 3E E8 64 29 F2\n" +
                        "B2 A6 02 AC 86 FE F6 EC B6 47 D6 29 5F AE 07 7A\n" +
                        "1F EB 51 75 08 D2 C1 6B 41 92 E0 1F 62\n"));
        }

        // Sample checksums:

        byte[] msg = hex(
                "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n" +
                "10 11 12 13 14");

        check(checksum(dk128, bk16, msg), hex(
                "D7 83 67 18 66 43 D6 7B 41 1C BA 91 39 FC 1D EE"));

        check(checksum(dk256, bk32, msg), hex(
                "45 EE 79 15 67 EE FC A3 7F 4A C1 E0 22 2D E8 0D\n" +
                "43 C3 BF A0 66 99 67 2A"));

        // Sample pseudorandom function (PRF) invocations:
        // Java does not support PRF. Skipped.
    }

    private static byte[] stringToKey(AesSha2DkCrypto dk,
            char[] pass, byte[] salt, byte[] params) throws Exception {
        Method m = AesSha2DkCrypto.class.getDeclaredMethod("stringToKey",
                char[].class, byte[].class, byte[].class);
        m.setAccessible(true);
        return (byte[])m.invoke(dk, pass, salt, params);
    }

    private static byte[] deriveKey(AesSha2DkCrypto dk, byte[] baseKey,
            int usage, byte type) throws Exception {
        Method m = AesSha2DkCrypto.class.getDeclaredMethod("deriveKey",
                byte[].class, int.class, byte.class);
        m.setAccessible(true);
        return (byte[]) m.invoke(dk, baseKey, usage, type);
    }

    private static byte[] cat(byte[] b1, byte[] b2) {
        byte[] result = Arrays.copyOf(b1, b1.length + b2.length);
        System.arraycopy(b2, 0, result, b1.length, b2.length);
        return result;
    }

    private static byte[] enc(AesSha2DkCrypto dk, byte[] confounder,
            byte[] bk, byte[] text) throws Exception {
        return dk.encryptRaw(bk, 2, new byte[16], cat(confounder, text),
                0, confounder.length + text.length);
    }

    private static byte[] checksum(AesSha2DkCrypto dk, byte[] baseKey, byte[] text)
            throws Exception {
        return dk.calculateChecksum(baseKey, 2, text, 0, text.length);
    }

    private static byte[] hex(String var) {
        var = var.replaceAll("\\s", "");
        return HexFormat.of().parseHex(var);
    }

    private static void check(byte[] b1, byte[] b2) throws Exception {
        if (!Arrays.equals(b1, b2)) {
            dump(b1); dump(b2);
            throw new Exception("Failure");
        }
    }

    private static void dump(byte[] data) throws Exception {
        HexPrinter.simple().dest(System.err).format(data);
    }
}
