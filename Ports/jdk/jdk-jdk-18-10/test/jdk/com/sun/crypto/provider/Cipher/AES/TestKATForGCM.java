/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6996769
 * @library ../UTIL
 * @build TestUtil
 * @run main TestKATForGCM
 * @summary Known Answer Test for AES cipher with GCM mode support in
 * SunJCE provider.
 * @author Valerie Peng
 */


import java.nio.ByteBuffer;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.math.*;

import java.util.*;

public class TestKATForGCM {

    // Utility methods
    private static byte[] HexToBytes(String hexVal) {
        if (hexVal == null) return new byte[0];
        byte[] result = new byte[hexVal.length()/2];
        for (int i = 0; i < result.length; i++) {
            // 2 characters at a time
            String byteVal = hexVal.substring(2*i, 2*i +2);
            result[i] = Integer.valueOf(byteVal, 16).byteValue();
        }
        return result;
    }

    private static class TestVector {
        int id;
        SecretKey key;
        byte[] plainText;
        byte[] aad;
        byte[] cipherText;
        byte[] tag;
        GCMParameterSpec spec;
        String info;

        TestVector(int id, String key, String iv, String pt, String aad,
                   String ct, String tag) {
            this.id = id;
            this.key = new SecretKeySpec(HexToBytes(key), "AES");
            this.plainText = HexToBytes(pt);
            this.aad = HexToBytes(aad);
            this.cipherText = HexToBytes(ct);
            this.tag = HexToBytes(tag);
            this.spec = new GCMParameterSpec(this.tag.length * 8, HexToBytes(iv));
            this.info = "id = " + id + ", key=" + key + ", iv=" + iv + ", pt=" + pt +
                ",aad=" + aad + ", ct=" + ct + ", tag=" + tag;
        }

        TestVector() {};

        TestVector duplicate() {
            TestVector t = new TestVector();
            t.id = id;
            t.key = key;
            t.plainText = plainText;
            t.cipherText = cipherText;
            t.aad = aad;
            t.tag = tag;
            t.spec = spec;
            t.info = info;
            return t;
        }

        public String toString() {
            return info;
        }
    }

    static boolean testFailed = false;

    // These test vectors are found off NIST's CAVP page
    // http://csrc.nist.gov/groups/STM/cavp/index.html
    // inside the link named "GCM Test Vectors", i.e.
    // http://csrc.nist.gov/groups/STM/cavp/documents/mac/gcmtestvectors.zip
    // CAVS 14.0, set of test vectors w/ count = 0, keysize = 128
    private static TestVector[] testValues = {
        // 96-bit iv w/ 128/120/112/104/96-bit tags
        // no plain text, no aad
        new TestVector(1, "11754cd72aec309bf52f7687212e8957",
                       "3c819d9a9bed087615030b65",
                       null, null, null,
                       "250327c674aaf477aef2675748cf6971"),
        new TestVector(2, "272f16edb81a7abbea887357a58c1917",
                       "794ec588176c703d3d2a7a07",
                       null, null, null,
                       "b6e6f197168f5049aeda32dafbdaeb"),
        new TestVector(3, "81b6844aab6a568c4556a2eb7eae752f",
                       "ce600f59618315a6829bef4d",
                       null, null, null,
                       "89b43e9dbc1b4f597dbbc7655bb5"),
        new TestVector(4, "cde2f9a9b1a004165ef9dc981f18651b",
                       "29512c29566c7322e1e33e8e",
                       null, null, null,
                       "2e58ce7dabd107c82759c66a75"),
        new TestVector(5, "b01e45cc3088aaba9fa43d81d481823f",
                       "5a2c4a66468713456a4bd5e1",
                       null, null, null,
                       "014280f944f53c681164b2ff"),
        // 96-bit iv w/ 128/120/112/104/96-bit tags
        // no plain text, 16-byte aad
        new TestVector(6, "77be63708971c4e240d1cb79e8d77feb",
                       "e0e00f19fed7ba0136a797f3",
                       null,
                       "7a43ec1d9c0a5a78a0b16533a6213cab",
                       null,
                       "209fcc8d3675ed938e9c7166709dd946"),
        new TestVector(7, "da0b615656135194ba6d3c851099bc48",
                       "d39d4b4d3cc927885090e6c3",
                       null,
                       "e7e5e6f8dac913036cb2ff29e8625e0e",
                       null,
                       "ab967711a5770461724460b07237e2"),
        new TestVector(8, "7e0986937a88eef894235aba4a2f43b2",
                       "92c4a631695907166b422d60",
                       null,
                       "85c185f8518f9f2cd597a8f9208fc76b",
                       null,
                       "3bb916b728df94fe9d1916736be1"),
        new TestVector(9, "c3db570d7f0c21e86b028f11465d1dc9",
                       "f86970f58ceef89fc7cb679e",
                       null,
                       "c095240708c0f57c288d86090ae34ee1",
                       null,
                       "e043c52160d652e82c7262fcf4"),
        new TestVector(10, "bea48ae4980d27f357611014d4486625",
                       "32bddb5c3aa998a08556454c",
                       null,
                       "8a50b0b8c7654bced884f7f3afda2ead",
                       null,
                       "8e0f6d8bf05ffebe6f500eb1"),
        // 96-bit iv w/ 128/120/112/104/96-bit tags
        // no plain text, 20-byte aad
        new TestVector(11, "2fb45e5b8f993a2bfebc4b15b533e0b4",
                       "5b05755f984d2b90f94b8027",
                       null,
                       "e85491b2202caf1d7dce03b97e09331c32473941",
                       null,
                       "c75b7832b2a2d9bd827412b6ef5769db"),
        new TestVector(12, "9bf406339fcef9675bbcf156aa1a0661",
                       "8be4a9543d40f542abacac95",
                       null,
                       "7167cbf56971793186333a6685bbd58d47d379b3",
                       null,
                       "5e7968d7bbd5ba58cfcc750e2ef8f1"),
        new TestVector(13, "a2e962fff70fd0f4d63be728b80556fc",
                       "1fa7103483de43d09bc23db4",
                       null,
                       "2a58edf1d53f46e4e7ee5e77ee7aeb60fc360658",
                       null,
                       "fa37f2dbbefab1451eae1d0d74ca"),
        new TestVector(14, "6bf4fdce82926dcdfc52616ed5f23695",
                       "cc0f5899a10615567e1193ed",
                       null,
                       "3340655592374c1da2f05aac3ee111014986107f",
                       null,
                       "8ad3385cce3b5e7c985908192c"),
        new TestVector(15, "4df7a13e43c3d7b66b1a72fac5ba398e",
                       "97179a3a2d417908dcf0fb28",
                       null,
                       "cbb7fc0010c255661e23b07dbd804b1e06ae70ac",
                       null,
                       "37791edae6c137ea946cfb40"),
        // 96-bit iv w/ 128-bit tags, 13/16/32/51-byte plain text, no aad
        new TestVector(16, "fe9bb47deb3a61e423c2231841cfd1fb",
                       "4d328eb776f500a2f7fb47aa",
                       "f1cc3818e421876bb6b8bbd6c9",
                       null,
                       "b88c5c1977b35b517b0aeae967",
                       "43fd4727fe5cdb4b5b42818dea7ef8c9"),
        new TestVector(17, "7fddb57453c241d03efbed3ac44e371c",
                       "ee283a3fc75575e33efd4887",
                       "d5de42b461646c255c87bd2962d3b9a2",
                       null,
                       "2ccda4a5415cb91e135c2a0f78c9b2fd",
                       "b36d1df9b9d5e596f83e8b7f52971cb3"),
        new TestVector(18, "9971071059abc009e4f2bd69869db338",
                       "07a9a95ea3821e9c13c63251",
                       "f54bc3501fed4f6f6dfb5ea80106df0bd836e6826225b75c0222f6e859b35983",
                       null,
                       "0556c159f84ef36cb1602b4526b12009c775611bffb64dc0d9ca9297cd2c6a01",
                       "7870d9117f54811a346970f1de090c41"),
        new TestVector(19, "594157ec4693202b030f33798b07176d",
                       "49b12054082660803a1df3df",

"3feef98a976a1bd634f364ac428bb59cd51fb159ec1789946918dbd50ea6c9d594a3a31a5269b0da6936c29d063a5fa2cc8a1c",
                      null,

"c1b7a46a335f23d65b8db4008a49796906e225474f4fe7d39e55bf2efd97fd82d4167de082ae30fa01e465a601235d8d68bc69",
                      "ba92d3661ce8b04687e8788d55417dc2"),
        // 96-bit iv w/ 128-bit tags, 16-byte plain text, 16/20/48/90-byte aad
        new TestVector(20, "c939cc13397c1d37de6ae0e1cb7c423c",
                       "b3d8cc017cbb89b39e0f67e2",
                       "c3b3c41f113a31b73d9a5cd432103069",
                       "24825602bd12a984e0092d3e448eda5f",
                       "93fe7d9e9bfd10348a5606e5cafa7354",
                       "0032a1dc85f1c9786925a2e71d8272dd"),
        new TestVector(21, "d4a22488f8dd1d5c6c19a7d6ca17964c",
                       "f3d5837f22ac1a0425e0d1d5",
                       "7b43016a16896497fb457be6d2a54122",
                       "f1c5d424b83f96c6ad8cb28ca0d20e475e023b5a",
                       "c2bd67eef5e95cac27e3b06e3031d0a8",
                       "f23eacf9d1cdf8737726c58648826e9c"),
        new TestVector(22, "89850dd398e1f1e28443a33d40162664",
                       "e462c58482fe8264aeeb7231",
                       "2805cdefb3ef6cc35cd1f169f98da81a",

"d74e99d1bdaa712864eec422ac507bddbe2b0d4633cd3dff29ce5059b49fe868526c59a2a3a604457bc2afea866e7606",
                       "ba80e244b7fc9025cd031d0f63677e06",
                       "d84a8c3eac57d1bb0e890a8f461d1065"),
        new TestVector(23, "bd7c5c63b7542b56a00ebe71336a1588",
                       "87721f23ba9c3c8ea5571abc",
                       "de15ddbb1e202161e8a79af6a55ac6f3",

"a6ec8075a0d3370eb7598918f3b93e48444751624997b899a87fa6a9939f844e008aa8b70e9f4c3b1a19d3286bf543e7127bfecba1ad17a5ec53fccc26faecacc4c75369498eaa7d706aef634d0009279b11e4ba6c993e5e9ed9",
                       "41eb28c0fee4d762de972361c863bc80",
                       "9cb567220d0b252eb97bff46e4b00ff8"),
        // 8/1024-bit iv w/ 128-bit tag, no plain text, no aad
        new TestVector(24, "1672c3537afa82004c6b8a46f6f0d026",
                       "05",
                       null, null, null,
                       "8e2ad721f9455f74d8b53d3141f27e8e"),
        new TestVector(25, "d0f1f4defa1e8c08b4b26d576392027c",

"42b4f01eb9f5a1ea5b1eb73b0fb0baed54f387ecaa0393c7d7dffc6af50146ecc021abf7eb9038d4303d91f8d741a11743166c0860208bcc02c6258fd9511a2fa626f96d60b72fcff773af4e88e7a923506e4916ecbd814651e9f445adef4ad6a6b6c7290cc13b956130eef5b837c939fcac0cbbcc9656cd75b13823ee5acdac",
                       null, null, null,
                       "7ab49b57ddf5f62c427950111c5c4f0d"),
        // 8-bit iv w/ 128-bit tag, 13-byte plain text, 90-byte aad
        new TestVector(26, "9f79239f0904eace50784b863e723f6b",
                       "d9",
                       "bdb0bb10c87965acd34d146171",

"44db436089327726c5f01139e1f339735c9e85514ccc2f167bad728010fb34a9072a9794c8a5e7361b1d0dbcdc9ac4091e354bb2896561f0486645252e9c78c86beece91bfa4f7cc4a8794ce1f305b1b735efdbf1ed1563c0be0",
                       "7e5a7c8dadb3f0c7335b4d9d8d",
                       "6b6ef1f53723a89f3bb7c6d043840717"),
        // 1024-bit iv w/ 128-bit tag, 51-byte plain text, 48-byte aad
        new TestVector(27, "141f1ce91989b07e7eb6ae1dbd81ea5e",

"49451da24bd6074509d3cebc2c0394c972e6934b45a1d91f3ce1d3ca69e194aa1958a7c21b6f21d530ce6d2cc5256a3f846b6f9d2f38df0102c4791e57df038f6e69085646007df999751e248e06c47245f4cd3b8004585a7470dee1690e9d2d63169a58d243c0b57b3e5b4a481a3e4e8c60007094ef3adea2e8f05dd3a1396f",

"d384305af2388699aa302f510913fed0f2cb63ba42efa8c5c9de2922a2ec2fe87719dadf1eb0aef212b51e74c9c5b934104a43",

"630cf18a91cc5a6481ac9eefd65c24b1a3c93396bd7294d6b8ba323951727666c947a21894a079ef061ee159c05beeb4",

"f4c34e5fbe74c0297313268296cd561d59ccc95bbfcdfcdc71b0097dbd83240446b28dc088abd42b0fc687f208190ff24c0548",
                      "dbb93bbb56d0439cd09f620a57687f5d"),
    };

    void executeArray(TestVector tv) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
        try {
            System.out.println("Test #" + tv.id + ": byte[].");

            c.init(Cipher.ENCRYPT_MODE, tv.key, tv.spec);
            c.updateAAD(tv.aad);
            byte[] ctPlusTag = c.doFinal(tv.plainText);

            c.init(Cipher.DECRYPT_MODE, tv.key, tv.spec);
            c.updateAAD(tv.aad);
            byte[] pt = c.doFinal(ctPlusTag); // should fail if tag mismatched

            // check encryption/decryption results just to be sure
            if (!Arrays.equals(tv.plainText, pt)) {
                System.out.println("PlainText diff failed for test# " + tv.id);
                testFailed = true;
            }
            int ctLen = tv.cipherText.length;
            if (!Arrays.equals(tv.cipherText,
                Arrays.copyOf(ctPlusTag, ctLen))) {
                System.out.println("CipherText diff failed for test# " + tv.id);
                testFailed = true;
            }
            int tagLen = tv.tag.length;
            if (!Arrays.equals
                (tv.tag,
                    Arrays.copyOfRange(ctPlusTag, ctLen, ctLen+tagLen))) {
                System.out.println("Tag diff failed for test# " + tv.id);
                testFailed = true;
            }
        } catch (Exception ex) {
            // continue testing other test vectors
            System.out.println("Failed Test Vector: " + tv);
            ex.printStackTrace();
            testFailed = true;
        }
        if (testFailed) {
            throw new Exception("Test Failed");
        }
    }

    void executeByteBuffer(TestVector tv, boolean direct, int offset) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");

        ByteBuffer src;
        ByteBuffer ctdst;
        ByteBuffer ptdst;

        if (direct) {
            System.out.print("Test #" + tv.id + ": ByteBuffer Direct.");
            src = ByteBuffer.allocateDirect(tv.plainText.length + offset);
            ctdst = ByteBuffer.allocateDirect(tv.cipherText.length + tv.tag.length + offset);
            ptdst = ByteBuffer.allocateDirect(tv.plainText.length + offset);
        } else {
            System.out.print("Test #" + tv.id + ": ByteBuffer Heap.");
            src = ByteBuffer.allocate(tv.plainText.length + offset);
            ctdst = ByteBuffer.allocate(tv.cipherText.length + tv.tag.length + offset);
            ptdst = ByteBuffer.allocate(tv.plainText.length + offset);
        }

        byte[] plainText;

        if (offset > 0) {
            System.out.println("  offset = " + offset);
            plainText = new byte[tv.plainText.length + offset];
            System.arraycopy(tv.plainText, 0, plainText, offset,
                tv.plainText.length);
        } else {
            System.out.println();
            plainText = tv.plainText;
        }

        src.put(plainText);
        src.position(offset);
        ctdst.position(offset);
        ctdst.mark();
        ptdst.position(offset);
        ptdst.mark();

        try {
            c.init(Cipher.ENCRYPT_MODE, tv.key, tv.spec);
            c.updateAAD(tv.aad);
            c.doFinal(src, ctdst);

            ctdst.reset();
            ByteBuffer tag = ctdst.duplicate();
            tag.position(tag.limit() - tv.tag.length);

            c.init(Cipher.DECRYPT_MODE, tv.key, tv.spec);
            c.updateAAD(tv.aad);
            c.doFinal(ctdst, ptdst); // should fail if tag mismatched

            ptdst.reset();
            // check encryption/decryption results just to be sure
            if (ptdst.compareTo(ByteBuffer.wrap(tv.plainText)) != 0) {
                System.out.println("\t PlainText diff failed for test# " + tv.id);
                testFailed = true;
            }

            ctdst.reset();
            ctdst.limit(ctdst.limit() - tv.tag.length);
            if (ctdst.compareTo(ByteBuffer.wrap(tv.cipherText)) != 0) {
                System.out.println("\t CipherText diff failed for test# " + tv.id);
                testFailed = true;
            }

            int mismatch = 0;
            for (int i = 0; i < tv.tag.length; i++) {
                mismatch |= tag.get() ^ tv.tag[i];
            }
            if (mismatch != 0) {
                System.out.println("\t Tag diff failed for test# " + tv.id);
                testFailed = true;
            }
        } catch (Exception ex) {
            // continue testing other test vectors
            System.out.println("\t Failed Test Vector ( #" + tv.id + ") : " + tv);
            ex.printStackTrace();
        }
    }

    public static void main (String[] args) throws Exception {
        TestKATForGCM test = new TestKATForGCM();
        for (TestVector tv : testValues) {
            test.executeArray(tv);
            test.executeByteBuffer(tv, false, 0);
            test.executeByteBuffer(tv, true, 0);
            test.executeByteBuffer(tv, false, 2);
            test.executeByteBuffer(tv, true, 2);
        }
        if (!testFailed) {
            System.out.println("Tests passed");
        }
    }
}

