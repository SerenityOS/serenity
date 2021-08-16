/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6862679
 * @modules java.security.jgss/sun.security.krb5
 *          java.security.jgss/sun.security.krb5.internal.crypto
 *          java.security.jgss/sun.security.krb5.internal.crypto.dk:+open
 * @run main/othervm RFC396xTest
 * @summary ESC: AD Authentication with user with umlauts fails
 */

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import sun.security.krb5.EncryptedData;
import sun.security.krb5.internal.crypto.Des;
import sun.security.krb5.internal.crypto.EType;
import sun.security.krb5.internal.crypto.crc32;
import sun.security.krb5.internal.crypto.dk.AesDkCrypto;
import sun.security.krb5.internal.crypto.dk.Des3DkCrypto;
import sun.security.krb5.internal.crypto.dk.DkCrypto;
import java.nio.*;
import java.util.HexFormat;
import javax.crypto.*;
import javax.crypto.spec.*;

public class RFC396xTest {

    static final String gclef = new String(Character.toChars(0x1d11e));

    /** Creates a new instance of NewClass */
    public static void main(String[] args) throws Exception {
        System.setProperty("sun.security.krb5.msinterop.des.s2kcharset",
                "utf-8");
        test();
    }

    static void test() throws Exception {
        // RFC 3961
        // A.1
        Method nfold = DkCrypto.class.getDeclaredMethod("nfold", byte[].class, Integer.TYPE);
        nfold.setAccessible(true);
        assertStringEquals(hex((byte[])nfold.invoke(null, "012345".getBytes("UTF-8"), 64)), "be072631276b1955");
        assertStringEquals(hex((byte[])nfold.invoke(null, "password".getBytes("UTF-8"), 56)), "78a07b6caf85fa");
        assertStringEquals(hex((byte[])nfold.invoke(null, "Rough Consensus, and Running Code".getBytes("UTF-8"), 64)), "bb6ed30870b7f0e0");
        assertStringEquals(hex((byte[])nfold.invoke(null, "password".getBytes("UTF-8"), 168)), "59e4a8ca7c0385c3c37b3f6d2000247cb6e6bd5b3e");
        assertStringEquals(hex((byte[])nfold.invoke(null, "MASSACHVSETTS INSTITVTE OF TECHNOLOGY".getBytes("UTF-8"), 192)), "db3b0d8f0b061e603282b308a50841229ad798fab9540c1b");
        assertStringEquals(hex((byte[])nfold.invoke(null, "Q".getBytes("UTF-8"), 168)), "518a54a215a8452a518a54a215a8452a518a54a215");
        assertStringEquals(hex((byte[])nfold.invoke(null, "ba".getBytes("UTF-8"), 168)), "fb25d531ae8974499f52fd92ea9857c4ba24cf297e");
        assertStringEquals(hex((byte[])nfold.invoke(null, "kerberos".getBytes("UTF-8"), 64)), "6b65726265726f73");
        assertStringEquals(hex((byte[])nfold.invoke(null, "kerberos".getBytes("UTF-8"), 128)), "6b65726265726f737b9b5b2b93132b93");
        assertStringEquals(hex((byte[])nfold.invoke(null, "kerberos".getBytes("UTF-8"), 168)), "8372c236344e5f1550cd0747e15d62ca7a5a3bcea4");
        assertStringEquals(hex((byte[])nfold.invoke(null, "kerberos".getBytes("UTF-8"), 256)), "6b65726265726f737b9b5b2b93132b935c9bdcdad95c9899c4cae4dee6d6cae4");

        // A.2
        assertStringEquals(hex(Des.string_to_key_bytes("passwordATHENA.MIT.EDUraeburn".toCharArray())), "cbc22fae235298e3");
        assertStringEquals(hex(Des.string_to_key_bytes("potatoeWHITEHOUSE.GOVdanny".toCharArray())), "df3d32a74fd92a01");
        assertStringEquals(hex(Des.string_to_key_bytes((gclef+"EXAMPLE.COMpianist").toCharArray())), "4ffb26bab0cd9413");
        assertStringEquals(hex(Des.string_to_key_bytes("\u00dfATHENA.MIT.EDUJuri\u0161i\u0107".toCharArray())), "62c81a5232b5e69d");
        // Next 2 won't pass, since there's no real weak key here
        //assertStringEquals(hex(Des.string_to_key_bytes("11119999AAAAAAAA".toCharArray())), "984054d0f1a73e31");
        //assertStringEquals(hex(Des.string_to_key_bytes("NNNN6666FFFFAAAA".toCharArray())), "c4bf6b25adf7a4f8");

        // A.3
        Object o = Des3DkCrypto.class.getConstructor().newInstance();
        Method dr = DkCrypto.class.getDeclaredMethod("dr", byte[].class, byte[].class);
        Method randomToKey = DkCrypto.class.getDeclaredMethod("randomToKey", byte[].class);
        dr.setAccessible(true);
        randomToKey.setAccessible(true);
        assertStringEquals(hex((byte[])randomToKey.invoke(o, (byte[])dr.invoke(o,
                xeh("dce06b1f64c857a11c3db57c51899b2cc1791008ce973b92"),
                xeh("0000000155")))),
                "925179d04591a79b5d3192c4a7e9c289b049c71f6ee604cd");
        assertStringEquals(hex((byte[])randomToKey.invoke(o, (byte[])dr.invoke(o,
                xeh("5e13d31c70ef765746578531cb51c15bf11ca82c97cee9f2"),
                xeh("00000001aa")))),
                "9e58e5a146d9942a101c469845d67a20e3c4259ed913f207");
        assertStringEquals(hex((byte[])randomToKey.invoke(o, (byte[])dr.invoke(o,
                xeh("98e6fd8a04a4b6859b75a176540b9752bad3ecd610a252bc"),
                xeh("0000000155")))),
                "13fef80d763e94ec6d13fd2ca1d085070249dad39808eabf");
        assertStringEquals(hex((byte[])randomToKey.invoke(o, (byte[])dr.invoke(o,
                xeh("622aec25a2fe2cad7094680b7c64940280084c1a7cec92b5"),
                xeh("00000001aa")))),
                "f8dfbf04b097e6d9dc0702686bcb3489d91fd9a4516b703e");
        assertStringEquals(hex((byte[])randomToKey.invoke(o, (byte[])dr.invoke(o,
                xeh("d3f8298ccb166438dcb9b93ee5a7629286a491f838f802fb"),
                xeh("6b65726265726f73")))),
                "2370da575d2a3da864cebfdc5204d56df779a7df43d9da43");
        assertStringEquals(hex((byte[])randomToKey.invoke(o, (byte[])dr.invoke(o,
                xeh("c1081649ada74362e6a1459d01dfd30d67c2234c940704da"),
                xeh("0000000155")))),
                "348057ec98fdc48016161c2a4c7a943e92ae492c989175f7");
        assertStringEquals(hex((byte[])randomToKey.invoke(o, (byte[])dr.invoke(o,
                xeh("5d154af238f46713155719d55e2f1f790dd661f279a7917c"),
                xeh("00000001aa")))),
                "a8808ac267dada3dcbe9a7c84626fbc761c294b01315e5c1");
        assertStringEquals(hex((byte[])randomToKey.invoke(o, (byte[])dr.invoke(o,
                xeh("798562e049852f57dc8c343ba17f2ca1d97394efc8adc443"),
                xeh("0000000155")))),
                "c813f88a3be3b334f75425ce9175fbe3c8493b89c8703b49");
        assertStringEquals(hex((byte[])randomToKey.invoke(o, (byte[])dr.invoke(o,
                xeh("26dce334b545292f2feab9a8701a89a4b99eb9942cecd016"),
                xeh("00000001aa")))),
                "f48ffd6e83f83e7354e694fd252cf83bfe58f7d5ba37ec5d");

        // A.4
        assertStringEquals(hex(new Des3DkCrypto().stringToKey("passwordATHENA.MIT.EDUraeburn".toCharArray())), "850bb51358548cd05e86768c313e3bfef7511937dcf72c3e");
        assertStringEquals(hex(new Des3DkCrypto().stringToKey("potatoeWHITEHOUSE.GOVdanny".toCharArray())), "dfcd233dd0a43204ea6dc437fb15e061b02979c1f74f377a");
        assertStringEquals(hex(new Des3DkCrypto().stringToKey("pennyEXAMPLE.COMbuckaroo".toCharArray())), "6d2fcdf2d6fbbc3ddcadb5da5710a23489b0d3b69d5d9d4a");
        assertStringEquals(hex(new Des3DkCrypto().stringToKey("\u00DFATHENA.MIT.EDUJuri\u0161i\u0107".toCharArray())), "16d5a40e1ce3bacb61b9dce00470324c831973a7b952feb0");
        assertStringEquals(hex(new Des3DkCrypto().stringToKey((gclef+"EXAMPLE.COMpianist").toCharArray())), "85763726585dbc1cce6ec43e1f751f07f1c4cbb098f40b19");

        // A.5
        assertStringEquals(hex(crc32.byte2crc32sum_bytes("foo".getBytes("UTF-8"))), "33bc3273");
        assertStringEquals(hex(crc32.byte2crc32sum_bytes("test0123456789".getBytes("UTF-8"))), "d6883eb8");
        assertStringEquals(hex(crc32.byte2crc32sum_bytes("MASSACHVSETTS INSTITVTE OF TECHNOLOGY".getBytes("UTF-8"))), "f78041e3");
        assertStringEquals(hex(crc32.byte2crc32sum_bytes(new byte[] {(byte)0x80, 0})), "4b98833b");
        assertStringEquals(hex(crc32.byte2crc32sum_bytes(new byte[] {0, 8})), "3288db0e");
        assertStringEquals(hex(crc32.byte2crc32sum_bytes(new byte[] {0, (byte)0x80})), "2083b8ed");
        assertStringEquals(hex(crc32.byte2crc32sum_bytes(new byte[] {(byte)0x80})), "2083b8ed");
        assertStringEquals(hex(crc32.byte2crc32sum_bytes(new byte[] {(byte)0x80, 0, 0, 0})), "3bb659ed");
        assertStringEquals(hex(crc32.byte2crc32sum_bytes(new byte[] {0, 0, 0, 1})), "96300777");

        // RFC 3962
        AesDkCrypto a1 = new AesDkCrypto(128);
        Method pbkdf2 = AesDkCrypto.class.getDeclaredMethod("PBKDF2", char[].class, byte[].class, Integer.TYPE, Integer.TYPE);
        Method s2k = AesDkCrypto.class.getDeclaredMethod("stringToKey", char[].class, byte[].class, byte[].class);
        pbkdf2.setAccessible(true);
        s2k.setAccessible(true);
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, "password".toCharArray(), "ATHENA.MIT.EDUraeburn".getBytes("UTF-8"), 1, 128)), "cd ed b5 28 1b b2 f8 01 56 5a 11 22 b2 56 35 15");
        assertStringEquals(hex(a1.stringToKey("password".toCharArray(), "ATHENA.MIT.EDUraeburn", i2b(1))), "42 26 3c 6e 89 f4 fc 28 b8 df 68 ee 09 79 9f 15");
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, "password".toCharArray(), "ATHENA.MIT.EDUraeburn".getBytes("UTF-8"), 1, 256)), "cd ed b5 28 1b b2 f8 01 56 5a 11 22 b2 56 35 15  0a d1 f7 a0 4b b9 f3 a3 33 ec c0 e2 e1 f7 08 37");
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, "password".toCharArray(), "ATHENA.MIT.EDUraeburn".getBytes("UTF-8"), 2, 128)), "01 db ee 7f 4a 9e 24 3e 98 8b 62 c7 3c da 93 5d");
        assertStringEquals(hex(a1.stringToKey("password".toCharArray(), "ATHENA.MIT.EDUraeburn", i2b(2))), "c6 51 bf 29 e2 30 0a c2 7f a4 69 d6 93 bd da 13");
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, "password".toCharArray(), "ATHENA.MIT.EDUraeburn".getBytes("UTF-8"), 2, 256)), "01 db ee 7f 4a 9e 24 3e 98 8b 62 c7 3c da 93 5d  a0 53 78 b9 32 44 ec 8f 48 a9 9e 61 ad 79 9d 86");
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, "password".toCharArray(), "ATHENA.MIT.EDUraeburn".getBytes("UTF-8"), 1200, 128)), "5c 08 eb 61 fd f7 1e 4e 4e c3 cf 6b a1 f5 51 2b");
        assertStringEquals(hex(a1.stringToKey("password".toCharArray(), "ATHENA.MIT.EDUraeburn", i2b(1200))), "4c 01 cd 46 d6 32 d0 1e 6d be 23 0a 01 ed 64 2a");
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, "password".toCharArray(), "ATHENA.MIT.EDUraeburn".getBytes("UTF-8"), 1200, 256)), "5c 08 eb 61 fd f7 1e 4e 4e c3 cf 6b a1 f5 51 2b  a7 e5 2d db c5 e5 14 2f 70 8a 31 e2 e6 2b 1e 13");
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, "password".toCharArray(), xeh("1234567878563412"), 5, 128)), "d1 da a7 86 15 f2 87 e6 a1 c8 b1 20 d7 06 2a 49");
        assertStringEquals(hex((byte[])s2k.invoke(a1, "password".toCharArray(), xeh("1234567878563412"), i2b(5))), "e9 b2 3d 52 27 37 47 dd 5c 35 cb 55 be 61 9d 8e");
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, "password".toCharArray(), xeh("1234567878563412"), 5, 256)), "d1 da a7 86 15 f2 87 e6 a1 c8 b1 20 d7 06 2a 49  3f 98 d2 03 e6 be 49 a6 ad f4 fa 57 4b 6e 64 ee");
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".toCharArray(), "pass phrase equals block size".getBytes("UTF-8"), 1200, 128)), "13 9c 30 c0 96 6b c3 2b a5 5f db f2 12 53 0a c9");
        assertStringEquals(hex(a1.stringToKey("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".toCharArray(), "pass phrase equals block size", i2b(1200))), "59 d1 bb 78 9a 82 8b 1a a5 4e f9 c2 88 3f 69 ed");
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".toCharArray(), "pass phrase equals block size".getBytes("UTF-8"), 1200, 256)), "13 9c 30 c0 96 6b c3 2b a5 5f db f2 12 53 0a c9  c5 ec 59 f1 a4 52 f5 cc 9a d9 40 fe a0 59 8e d1");
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".toCharArray(), "pass phrase exceeds block size".getBytes("UTF-8"), 1200, 128)), "9c ca d6 d4 68 77 0c d5 1b 10 e6 a6 87 21 be 61");
        assertStringEquals(hex(a1.stringToKey("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".toCharArray(), "pass phrase exceeds block size", i2b(1200))), "cb 80 05 dc 5f 90 17 9a 7f 02 10 4c 00 18 75 1d");
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".toCharArray(), "pass phrase exceeds block size".getBytes("UTF-8"), 1200, 256)), "9c ca d6 d4 68 77 0c d5 1b 10 e6 a6 87 21 be 61  1a 8b 4d 28 26 01 db 3b 36 be 92 46 91 5e c8 2a");
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, gclef.toCharArray(), "EXAMPLE.COMpianist".getBytes("UTF-8"), 50, 128)), "6b 9c f2 6d 45 45 5a 43 a5 b8 bb 27 6a 40 3b 39");
        assertStringEquals(hex(a1.stringToKey(gclef.toCharArray(), "EXAMPLE.COMpianist", i2b(50))), "f1 49 c1 f2 e1 54 a7 34 52 d4 3e 7f e6 2a 56 e5");
        assertStringEquals(hex((byte[])pbkdf2.invoke(null, gclef.toCharArray(), "EXAMPLE.COMpianist".getBytes("UTF-8"), 50, 256)), "6b 9c f2 6d 45 45 5a 43 a5 b8 bb 27 6a 40 3b 39  e7 fe 37 a0 c4 1e 02 c2 81 ff 30 69 e1 e9 4f 52");

        if (EType.isSupported(EncryptedData.ETYPE_AES256_CTS_HMAC_SHA1_96)) {
            AesDkCrypto a2 = new AesDkCrypto(256);
            assertStringEquals(hex(a2.stringToKey("password".toCharArray(), "ATHENA.MIT.EDUraeburn", i2b(1))), "fe 69 7b 52 bc 0d 3c e1 44 32 ba 03 6a 92 e6 5b  bb 52 28 09 90 a2 fa 27 88 39 98 d7 2a f3 01 61");
            assertStringEquals(hex(a2.stringToKey("password".toCharArray(), "ATHENA.MIT.EDUraeburn", i2b(2))), "a2 e1 6d 16 b3 60 69 c1 35 d5 e9 d2 e2 5f 89 61  02 68 56 18 b9 59 14 b4 67 c6 76 22 22 58 24 ff");
            assertStringEquals(hex(a2.stringToKey("password".toCharArray(), "ATHENA.MIT.EDUraeburn", i2b(1200))), "55 a6 ac 74 0a d1 7b 48 46 94 10 51 e1 e8 b0 a7  54 8d 93 b0 ab 30 a8 bc 3f f1 62 80 38 2b 8c 2a");
            assertStringEquals(hex((byte[])s2k.invoke(a2, "password".toCharArray(), xeh("1234567878563412"), i2b(5))), "97 a4 e7 86 be 20 d8 1a 38 2d 5e bc 96 d5 90 9c  ab cd ad c8 7c a4 8f 57 45 04 15 9f 16 c3 6e 31");
            assertStringEquals(hex(a2.stringToKey("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".toCharArray(), "pass phrase equals block size", i2b(1200))), "89 ad ee 36 08 db 8b c7 1f 1b fb fe 45 94 86 b0  56 18 b7 0c ba e2 20 92 53 4e 56 c5 53 ba 4b 34");
            assertStringEquals(hex(a2.stringToKey("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX".toCharArray(), "pass phrase exceeds block size", i2b(1200))), "d7 8c 5c 9c b8 72 a8 c9 da d4 69 7f 0b b5 b2 d2 14 96 c8 2b eb 2c ae da 21 12 fc ee a0 57 40 1b");
            assertStringEquals(hex(a2.stringToKey(gclef.toCharArray(), "EXAMPLE.COMpianist", i2b(50))), "4b 6d 98 39 f8 44 06 df 1f 09 cc 16 6d b4 b8 3c  57 18 48 b7 84 a3 d6 bd c3 46 58 9a 3e 39 3f 9e");
        }

        Cipher cipher = Cipher.getInstance("AES/CTS/NoPadding");

        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(
                xeh("63 68 69 63 6b 65 6e 20 74 65 72 69 79 61 6b 69"), "AES"),
                new IvParameterSpec(
                xeh("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"), 0, 16));
        assertStringEquals(hex(cipher.doFinal(
                xeh("49 20 77 6f 75 6c 64 20 6c 69 6b 65 20 74 68 65 20"))),
                "c6 35 35 68 f2 bf 8c b4 d8 a5 80 36 2d a7 ff 7f  97");
        assertStringEquals(hex(cipher.doFinal(
                xeh("49 20 77 6f 75 6c 64 20 6c 69 6b 65 20 74 68 65  20 47 65 6e 65 72 61 6c 20 47 61 75 27 73 20"))),
                "fc 00 78 3e 0e fd b2 c1 d4 45 d4 c8 ef f7 ed 22  97 68 72 68 d6 ec cc c0 c0 7b 25 e2 5e cf e5");
        assertStringEquals(hex(cipher.doFinal(
                xeh("49 20 77 6f 75 6c 64 20 6c 69 6b 65 20 74 68 65  20 47 65 6e 65 72 61 6c 20 47 61 75 27 73 20 43"))),
                "39 31 25 23 a7 86 62 d5 be 7f cb cc 98 eb f5 a8  97 68 72 68 d6 ec cc c0 c0 7b 25 e2 5e cf e5 84");
        assertStringEquals(hex(cipher.doFinal(
                xeh("49 20 77 6f 75 6c 64 20 6c 69 6b 65 20 74 68 65  20 47 65 6e 65 72 61 6c 20 47 61 75 27 73 20 43  68 69 63 6b 65 6e 2c 20 70 6c 65 61 73 65 2c"))),
                "97 68 72 68 d6 ec cc c0 c0 7b 25 e2 5e cf e5 84  b3 ff fd 94 0c 16 a1 8c 1b 55 49 d2 f8 38 02 9e  39 31 25 23 a7 86 62 d5 be 7f cb cc 98 eb f5");
        assertStringEquals(hex(cipher.doFinal(
                xeh("49 20 77 6f 75 6c 64 20 6c 69 6b 65 20 74 68 65  20 47 65 6e 65 72 61 6c 20 47 61 75 27 73 20 43  68 69 63 6b 65 6e 2c 20 70 6c 65 61 73 65 2c 20"))),
                "97 68 72 68 d6 ec cc c0 c0 7b 25 e2 5e cf e5 84  9d ad 8b bb 96 c4 cd c0 3b c1 03 e1 a1 94 bb d8  39 31 25 23 a7 86 62 d5 be 7f cb cc 98 eb f5 a8");
        assertStringEquals(hex(cipher.doFinal(
                xeh("49 20 77 6f 75 6c 64 20 6c 69 6b 65 20 74 68 65  20 47 65 6e 65 72 61 6c 20 47 61 75 27 73 20 43  68 69 63 6b 65 6e 2c 20 70 6c 65 61 73 65 2c 20  61 6e 64 20 77 6f 6e 74 6f 6e 20 73 6f 75 70 2e"))),
                "97 68 72 68 d6 ec cc c0 c0 7b 25 e2 5e cf e5 84  39 31 25 23 a7 86 62 d5 be 7f cb cc 98 eb f5 a8  48 07 ef e8 36 ee 89 a5 26 73 0d bc 2f 7b c8 40  9d ad 8b bb 96 c4 cd c0 3b c1 03 e1 a1 94 bb d8");
    }

    static byte[] i2b(int i) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        byte[] b = new byte[4];
        bb.putInt(i);
        bb.flip();
        bb.get(b);
        return b;
    }

    static String hex(byte[] bs) {
        return HexFormat.of().formatHex(bs);
    }

    static byte[] xeh(String in) {
        in = in.replaceAll(" ", "");
        return HexFormat.of().parseHex(in);
    }

    static void assertStringEquals(String a, String b) {
        a = a.replaceAll(" ", "");
        b = b.replaceAll(" ", "");
        if (!a.equals(b)) {
            throw new RuntimeException("Not equal: " + a + " AND " + b);
        }
        System.err.print(".");
    }
}
