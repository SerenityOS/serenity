/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 0000000
 * @summary This test checks performance of various ciphers.
 * @author Jan Luehe
 * @run main/manual PerformanceTest
 */
import java.security.*;
import java.security.spec.*;
import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class PerformanceTest {

    Cipher cipher;
    IvParameterSpec params = null;
    SecretKey cipherKey = null;
    StringBuffer col;

    public static byte[] key = {
        (byte)0x01,(byte)0x23,(byte)0x45,(byte)0x67,
        (byte)0x89,(byte)0xab,(byte)0xcd,(byte)0xef
    };

    public static byte[] key3 = {
        (byte)0x01,(byte)0x23,(byte)0x45,(byte)0x67,
        (byte)0x89,(byte)0xab,(byte)0xcd,(byte)0xef,
        (byte)0xf0,(byte)0xe1,(byte)0xd2,(byte)0xc3,
        (byte)0xb4,(byte)0xa5,(byte)0x96,(byte)0x87,
        (byte)0xfe,(byte)0xdc,(byte)0xba,(byte)0x98,
        (byte)0x76,(byte)0x54,(byte)0x32,(byte)0x10};

    public static byte[] iv  = {
        (byte)0xfe,(byte)0xdc,(byte)0xba,(byte)0x98,
        (byte)0x76,(byte)0x54,(byte)0x32,(byte)0x10};

    public static byte[] plain_data =
            ("Isaiah, a little boy is dead. He fell from the roof of an apartment house, "
            + "just a few days before Christmas. The only person who truly grieves for "
            + "the boy is Smilla Jasperson, a polar researcher. Smilla was Isaiah's "
            + "only friend. Isaiah came with his alcoholic mother from Greenland but "
            + "never really got used to life in Copenhagen. Smilla's mother was also "
            + "from Greenland. Smilla feels a particular affinity to the arctic landscape. "
            + "She knows a lot about the snow and how to read its movements and is "
            + "convinced that the boy's death was not accidental.").getBytes();

    static int[] dataSizes = {1024, 8192};
    static String[] crypts = {"DES", "DESede"};
    static String[] modes = {"ECB", "CBC", "CFB64", "OFB64", "PCBC"};
    static String[] paddings = {"PKCS5Padding", "NoPadding"};
    static int[] rounds = {100, 1000};
    int sum =0;

    public static void main(String[] args) throws Exception {
        PerformanceTest test = new PerformanceTest();
        test.run();
    }

    public void run() throws Exception {

        byte[] in;

        col = new StringBuffer();

        printHeadings();

        for (int i=0; i<crypts.length; i++) {
            for (int j=0; j<modes.length; j++) {
                for (int k=0; k<paddings.length; k++) {

                    col.append(crypts[i]+"/"+modes[j]+"/"+paddings[k]);
                    int len = 32 - col.length();
                    for(; len>0; len--)
                        col.append(" "); {

                        for (int l=0; l<dataSizes.length; l++) {

                            in = new byte[dataSizes[l]];
                            int g = Math.min(dataSizes[l],
                                             plain_data.length);
                            for(len=0; len < dataSizes[l] - g; len += g) {
                                System.arraycopy
                                    (plain_data, 0, in, len, g);
                            }

                            if ((dataSizes[l] - len) > 0)
                                System.arraycopy(plain_data, 0,
                                                 in, len, dataSizes[l]- len);

                            col.append(dataSizes[l]);
                            len = 40 - col.length();
                            for (; len>0; len--)
                                col.append(" ");

                            for (int m=0; m<rounds.length; m++) {

                                col.append(rounds[m]);
                                len = 50 - col.length();
                                for (; len>0; len--)
                                    col.append(" ");

                                init(crypts[i], modes[j], paddings[k]);
                                runTest(in, rounds[m]);
                                System.out.println(col.toString());
                                col.setLength(40);
                            }
                            col.setLength(32);
                        }
                        col.setLength(0);
                        col.append("Average:                                            " + (sum/(dataSizes.length*rounds.length)));
                        sum = 0;
                        System.out.println(col.toString() + "\n");
                        col.setLength(0);
                    }
                }
            }
        }
    }

    public void init(String crypt, String mode, String padding)
        throws Exception {

        KeySpec desKeySpec = null;
        SecretKeyFactory factory = null;

        StringBuffer cipherName = new StringBuffer(crypt);
        if (mode.length() != 0)
            cipherName.append("/" + mode);
        if (padding.length() != 0)
            cipherName.append("/" + padding);

        cipher = Cipher.getInstance(cipherName.toString(), "SunJCE");
        if (crypt.endsWith("ede")) {
            desKeySpec = new DESedeKeySpec(key3);
            factory = SecretKeyFactory.getInstance("DESede", "SunJCE");
        }
        else {
            desKeySpec = new DESKeySpec(key);
            factory = SecretKeyFactory.getInstance("DES", "SunJCE");
        }

        // retrieve the cipher key
        cipherKey = factory.generateSecret(desKeySpec);

        // retrieve iv
        if ( !mode.equals("ECB"))
            params = new IvParameterSpec(iv);
        else
            params = null;
    }

    public void runTest(byte[] data, int count) throws Exception {

        long start, end;
        cipher.init(Cipher.ENCRYPT_MODE, cipherKey, params);

        start = System.currentTimeMillis();
        for (int i=0; i<count-1; i++) {
            cipher.update(data, 0, data.length);
        }
        cipher.doFinal(data, 0, data.length);
        end = System.currentTimeMillis();

        int speed = (int)((data.length * count)/(end - start));
        sum += speed;
        col.append(speed);
    }

    public void printHeadings() {
        System.out.println
            ("The following tests numbers are generated using");
        System.out.println
            ("our JCA calling through Cipher to a particular provider");
        System.out.println
            ("=========================================================");
        System.out.println
            ("Algorithm                      DataSize Rounds Kbytes/sec");

    }

}
