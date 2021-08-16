/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary DesAPITest
 * @author Jan Luehe
 */
import java.io.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class DesAPITest {

    Cipher cipher;
    IvParameterSpec params = null;
    SecretKey cipherKey = null;

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

    static String[] crypts = {"DES", "DESede"};
    //static String[] modes = {"ECB", "CBC", "CFB", "OFB", "PCBC"};
    static String[] modes = {"CFB24"};
    //static String[] paddings = {"PKCS5Padding", "NoPadding"};
    static String[] paddings = {"PKCS5Padding"};

    public static void main(String[] args) throws Exception {
        DesAPITest test = new DesAPITest();
        test.run();
    }

    public void run() throws Exception {

        for (int i=0; i<crypts.length; i++) {
            for (int j=0; j<modes.length; j++) {
                for (int k=0; k<paddings.length; k++) {
                    System.out.println
                        ("===============================");
                    System.out.println
                        (crypts[i]+" "+modes[j]+" " + paddings[k]);
                    init(crypts[i], modes[j], paddings[k]);
                    runTest();
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

    public void runTest() throws Exception {

        int bufferLen = 512;
        byte[] input = new byte[bufferLen];
        int len;

        // encrypt test
        cipher.init(Cipher.ENCRYPT_MODE, cipherKey, params);

        // getIV
        System.out.println("getIV, " + cipher.getIV());
        byte[] output = null;
        boolean thrown = false;
        try {
            input = null;
            output = cipher.update(input, 0, -1);
        } catch (IllegalArgumentException ex) {
            thrown = true;
        }
        if (!thrown) {
            throw new Exception("Expected IAE not thrown!");
        }
        byte[] inbuf = "itaoti7890123456".getBytes();
        System.out.println("inputLength: " + inbuf.length);
        output = cipher.update(inbuf);

        len = cipher.getOutputSize(16);
        byte[] out = new byte[len];
        output = cipher.doFinal();
        System.out.println(len + " " + TestUtility.hexDump(output));
    }
}
