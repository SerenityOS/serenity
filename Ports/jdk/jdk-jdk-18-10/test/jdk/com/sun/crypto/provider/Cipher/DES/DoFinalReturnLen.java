/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Makes sure that Cipher.doFinal() returns the right number
 *      of bytes written
 * @author Jan Luehe
 */
import java.security.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class DoFinalReturnLen {
    static byte[] dataToEncrypt = {
        (byte)0x02, (byte)0xA4, (byte)0x20, (byte)0x87,
        (byte)0xB6, (byte)0xC4, (byte)0x54, (byte)0x89,
        (byte)0xA2, (byte)0xA4, (byte)0x10, (byte)0x87,
        (byte)0xCD, (byte)0x76, (byte)0x43, (byte)0xF0,
        (byte)0x72, (byte)0xA4, (byte)0xA0, (byte)0x82,
        (byte)0x26, (byte)0xC4, (byte)0x54, (byte)0x19,
        (byte)0x09, (byte)0xAC, (byte)0x2A, (byte)0xE7,
        (byte)0x1D, (byte)0x74, (byte)0x13, (byte)0x20,
        (byte)0xAD, (byte)0xD4, (byte)0xD0, (byte)0x87,
        (byte)0xB6, (byte)0xC5, (byte)0x24, (byte)0x89,
        (byte)0x02, (byte)0xF4, (byte)0x90, (byte)0x8E,
        (byte)0x5D, (byte)0x76, (byte)0x43, (byte)0x2F,
        (byte)0x03, (byte)0xAD, (byte)0x20, (byte)0x8C,
        (byte)0xB5, (byte)0xC4, (byte)0xA4, (byte)0x39,
        (byte)0xD2, (byte)0xA4, (byte)0xE0, (byte)0x87,
        (byte)0xCD, (byte)0x56, (byte)0x53, (byte)0x20
    };

    static byte[] dataToEncryptUneven = {
        (byte)0x02, (byte)0xA4, (byte)0x20, (byte)0x87,
        (byte)0xB6, (byte)0xC4, (byte)0x54, (byte)0x89,
        (byte)0xA2, (byte)0xA4, (byte)0x10, (byte)0x87,
        (byte)0xCD, (byte)0x76, (byte)0x43, (byte)0xF0,
        (byte)0x72, (byte)0xA4, (byte)0xA0, (byte)0x82,
        (byte)0x26, (byte)0xC4, (byte)0x54, (byte)0x19,
        (byte)0x09, (byte)0xAC, (byte)0x2A, (byte)0xE7,
        (byte)0x1D, (byte)0x74, (byte)0x13, (byte)0x20,
        (byte)0xAD, (byte)0xD4, (byte)0xD0, (byte)0x87,
        (byte)0xB6, (byte)0xC5, (byte)0x24, (byte)0x89,
        (byte)0x02, (byte)0xF4, (byte)0x90, (byte)0x8E,
        (byte)0x5D, (byte)0x76, (byte)0x43, (byte)0x2F,
        (byte)0x03, (byte)0xAD, (byte)0x20, (byte)0x8C,
        (byte)0xB5, (byte)0xC4, (byte)0xA4, (byte)0x39,
        (byte)0xD2, (byte)0xA4, (byte)0xE0, (byte)0x87,
        (byte)0xCD, (byte)0x56, (byte)0x53, (byte)0x20,
        (byte)0x22
    };

    static byte[] iv = {
        (byte)0x03, (byte)0xAD, (byte)0x20, (byte)0x8C,
        (byte)0xB5, (byte)0xC4, (byte)0xA4, (byte)0x39
    };

    public static void main (String args[]) throws Exception {
        byte[] encryptedData = null;
        byte[] decryptedData = null;
        Cipher newDES = null;
        IvParameterSpec IvParamSpec = null;
        SecretKey sKey = null;

        // Step 0: list providers
        Provider[] theProviders = Security.getProviders();
        for (int index = 0; index < theProviders.length; index++) {
            System.out.println(theProviders[index].getName());
            System.out.println(theProviders[index].getVersion());
            System.out.println(theProviders[index].getInfo());
        }

        // Cipher Object
        newDES = Cipher.getInstance("DES/CBC/PKCS5Padding",
                                    "SunJCE");
        byte[] keyData = {
            (byte)0x46, (byte)0x19, (byte)0x20, (byte)0x5e,
            (byte)0xef, (byte)0x0b, (byte)0x7c, (byte)0x45
        };
        // Generate secret key
        SecretKeyFactory desFactory
            = SecretKeyFactory.getInstance("DES", "SunJCE");
        DESKeySpec keySpec = new DESKeySpec(keyData);
        sKey = desFactory.generateSecret(keySpec);

        // encrypt data
        System.out.println("DataToEncrypt:");
        printBuffer(dataToEncrypt);

        IvParamSpec = new IvParameterSpec(iv, 0, 8);
        newDES.init(Cipher.ENCRYPT_MODE, sKey, IvParamSpec);

        encryptedData =
            new byte[newDES.getOutputSize(dataToEncrypt.length)];
        int outputLenUpdate = newDES.update(dataToEncrypt, 0,
                                            dataToEncrypt.length,
                                            encryptedData);
        int outputLenFinal = newDES.doFinal(encryptedData,
                                            outputLenUpdate);

        System.out.println("ENCRYPT : Update " + outputLenUpdate
                           + " bytes");
        System.out.println("ENCRYPT : Final " + outputLenFinal
                           + " bytes");

        System.out.println("Encrypted data:");
        printBuffer(encryptedData);

        // decrypt data
        newDES.init(Cipher.DECRYPT_MODE, sKey, IvParamSpec);

        decryptedData =
            new byte[newDES.getOutputSize(encryptedData.length)];
        System.out.println("encrLen: " + encryptedData.length);
        System.out.println("decrLen: " + decryptedData.length);

        outputLenUpdate = newDES.update(encryptedData, 0,
                                        encryptedData.length,
                                        decryptedData, 0);

        outputLenFinal = newDES.doFinal(decryptedData,
                                        outputLenUpdate);

        System.out.println("DECRYPT : Update " + outputLenUpdate
                           + " bytes");
        System.out.println("DECRYPT : Final " + outputLenFinal
                           + " bytes");

        System.out.println("Decrypted data:");
        printBuffer(decryptedData);
        int len = 0;
        if (dataToEncrypt.length >= decryptedData.length)
            len = decryptedData.length;
        else
            len = dataToEncrypt.length;
        for (int i=0; i<len; i++)
            if (dataToEncrypt[i] != decryptedData[i])
                throw new Exception("Original and recovered data differ");

        // decrypt data with exact output buffer length
        newDES.init(Cipher.DECRYPT_MODE, sKey, IvParamSpec);

        decryptedData = new byte[dataToEncrypt.length];
        System.out.println("encrLen: " + encryptedData.length);
        System.out.println("decrLen: " + decryptedData.length);

        outputLenUpdate = newDES.update(encryptedData, 0,
                                        encryptedData.length,
                                        decryptedData, 0);

        outputLenFinal = newDES.doFinal(decryptedData,
                                        outputLenUpdate);

        System.out.println("DECRYPT : Update " + outputLenUpdate
                           + " bytes");
        System.out.println("DECRYPT : Final " + outputLenFinal
                           + " bytes");

        System.out.println("Decrypted data:");
        printBuffer(decryptedData);
        len = 0;
        if (dataToEncrypt.length >= decryptedData.length)
            len = decryptedData.length;
        else
            len = dataToEncrypt.length;
        for (int i=0; i<len; i++)
            if (dataToEncrypt[i] != decryptedData[i])
                throw new Exception("Original and recovered data differ");

        //
        // run the same test for the input data with uneven number of bytes
        //

        // encrypt data
        System.out.println();
        System.out.println("DataToEncrypt:");
        printBuffer(dataToEncryptUneven);

        newDES.init(Cipher.ENCRYPT_MODE, sKey, IvParamSpec);

        encryptedData =
            new byte[newDES.getOutputSize(dataToEncryptUneven.length)];
        outputLenUpdate = newDES.update(dataToEncryptUneven, 0,
                                        dataToEncryptUneven.length,
                                        encryptedData);
        outputLenFinal = newDES.doFinal(encryptedData,
                                        outputLenUpdate);

        System.out.println("ENCRYPT : Update " + outputLenUpdate
                           + " bytes");
        System.out.println("ENCRYPT : Final " + outputLenFinal
                           + " bytes");

        System.out.println("Encrypted data:");
        printBuffer(encryptedData);

        // decrypt data
        newDES.init(Cipher.DECRYPT_MODE, sKey, IvParamSpec);

        decryptedData =
            new byte[newDES.getOutputSize(encryptedData.length)];
        System.out.println("encrLen: " + encryptedData.length);
        System.out.println("decrLen: " + decryptedData.length);

        outputLenUpdate = newDES.update(encryptedData, 0,
                                        encryptedData.length,
                                        decryptedData, 0);

        outputLenFinal = newDES.doFinal(decryptedData,
                                        outputLenUpdate);

        System.out.println("DECRYPT : Update " + outputLenUpdate
                           + " bytes");
        System.out.println("DECRYPT : Final " + outputLenFinal
                           + " bytes");

        System.out.println("Decrypted data:");
        printBuffer(decryptedData);
        len = 0;
        if (dataToEncryptUneven.length >= decryptedData.length)
            len = decryptedData.length;
        else
            len = dataToEncryptUneven.length;
        for (int i=0; i<len; i++)
            if (dataToEncryptUneven[i] != decryptedData[i])
                throw new Exception("Original and recovered data differ");

        // decrypt data with exact output buffer length
        newDES.init(Cipher.DECRYPT_MODE, sKey, IvParamSpec);

        decryptedData = new byte[dataToEncryptUneven.length];
        System.out.println("encrLen: " + encryptedData.length);
        System.out.println("decrLen: " + decryptedData.length);

        outputLenUpdate = newDES.update(encryptedData, 0,
                                        encryptedData.length,
                                        decryptedData, 0);

        outputLenFinal = newDES.doFinal(decryptedData,
                                        outputLenUpdate);

        System.out.println("DECRYPT : Update " + outputLenUpdate
                           + " bytes");
        System.out.println("DECRYPT : Final " + outputLenFinal
                           + " bytes");

        System.out.println("Decrypted data:");
        printBuffer(decryptedData);
        len = 0;
        if (dataToEncryptUneven.length >= decryptedData.length)
            len = decryptedData.length;
        else
            len = dataToEncryptUneven.length;
        for (int i=0; i<len; i++)
            if (dataToEncryptUneven[i] != decryptedData[i])
                throw new Exception("Original and recovered data differ");

        System.out.println();
        System.out.println("Test succeeded");
    }

    // These methods print out a byte array in a reasonably pretty format.
    private static void printBuffer(byte[] byteArray) {
        printBuffer(byteArray, byteArray.length);
    }

    private static void printBuffer(byte[] byteArray, int length) {
        StringBuffer textLine = new StringBuffer("                ");
        System.out.print("; 00000000: ");
        for (int i=0; i < length; i++) {
            if (((i%16) == 0) && i != 0) {
                System.out.println("[" + textLine + "]");
                System.out.print("; ");
                hexPrint(i, 8);
                System.out.print(": ");
                for(int j=0; j<16; j++) {
                    textLine.setCharAt(j,' ');
                }
            }
            hexPrint((int) byteArray[i], 2);
            System.out.print(" ");
            if ((byteArray[i] < 32) || (byteArray[i] > 127)) {
                textLine.setCharAt(i%16, '.');
            } else {
                textLine.setCharAt(i%16,(char)byteArray[i]);
            }
        }
        if (((length % 16) != 0) || (length == 0)) {
            for (int i = 0; i < 16 - (length % 16); i++) {
                System.out.print("   ");
            }
        }
        System.out.println("[" + textLine + "]");
    }

    private static void hexPrint(int value, int padding) {
        String hexString = new String("0123456789ABCDEF");
        for (int i = (padding - 1); i >= 0; i--) {
            System.out.print(hexString.charAt((value >> (i*4)) & 0xF));
        }
    }
}
