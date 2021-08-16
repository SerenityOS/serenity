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
 * @bug 8012637
 * @library ../UTIL
 * @build TestUtil
 * @run main TestCICOWithGCM
 * @summary Test CipherInputStream/OutputStream with AES GCM mode.
 * @author Valerie Peng
 * @key randomness
 */

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.math.*;
import java.io.*;

import java.util.*;

public class TestCICOWithGCM {
    public static void main(String[] args) throws Exception {
        //init Secret Key
        KeyGenerator kg = KeyGenerator.getInstance("AES", "SunJCE");
        kg.init(128);
        SecretKey key = kg.generateKey();

        //do initialization of the plainText
        byte[] plainText = new byte[800];
        Random rdm = new Random();
        rdm.nextBytes(plainText);

        //init ciphers
        Cipher encCipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
        encCipher.init(Cipher.ENCRYPT_MODE, key);
        Cipher decCipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
        decCipher.init(Cipher.DECRYPT_MODE, key, encCipher.getParameters());

        //init cipher streams
        ByteArrayInputStream baInput = new ByteArrayInputStream(plainText);
        CipherInputStream ciInput = new CipherInputStream(baInput, encCipher);
        ByteArrayOutputStream baOutput = new ByteArrayOutputStream();
        CipherOutputStream ciOutput = new CipherOutputStream(baOutput, decCipher);

        //do test
        byte[] buffer = new byte[800];
        int len = ciInput.read(buffer);
        System.out.println("read " + len + " bytes from input buffer");

        while (len != -1) {
            ciOutput.write(buffer, 0, len);
            System.out.println("wite " + len + " bytes to output buffer");
            len = ciInput.read(buffer);
            if (len != -1) {
                System.out.println("read " + len + " bytes from input buffer");
            } else {
                System.out.println("finished reading");
            }
        }

        ciOutput.flush();
        ciInput.close();
        ciOutput.close();
        byte[] recovered = baOutput.toByteArray();
        System.out.println("recovered " + recovered.length + " bytes");
        if (!Arrays.equals(plainText, recovered)) {
            throw new RuntimeException("diff check failed!");
        } else {
            System.out.println("diff check passed");
        }
    }
}
