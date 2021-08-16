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
 * @summary FlushBug
 * @author Jan Luehe
 * @key randomness
 */
import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class FlushBug {
    public static void main(String[] args) throws Exception {
        SecureRandom sr = new SecureRandom();

        // Create new DES key.
        KeyGenerator kg = KeyGenerator.getInstance("DES", "SunJCE");
        kg.init(sr);
        Key key = kg.generateKey();

        // Generate an IV.
        byte[] iv_bytes = new byte[8];
        sr.nextBytes(iv_bytes);
        IvParameterSpec iv = new IvParameterSpec(iv_bytes);

        // Create the consumer
        Cipher decrypter = Cipher.getInstance("DES/CFB8/NoPadding", "SunJCE");
        decrypter.init(Cipher.DECRYPT_MODE, key, iv);
        PipedInputStream consumer = new PipedInputStream();
        InputStream in = new CipherInputStream(consumer, decrypter);

        // Create the producer
        Cipher encrypter = Cipher.getInstance("DES/CFB8/NoPadding", "SunJCE");
        encrypter.init(Cipher.ENCRYPT_MODE, key, iv);
        PipedOutputStream producer = new PipedOutputStream();
        OutputStream out = new CipherOutputStream(producer, encrypter);

        producer.connect(consumer); // connect pipe

        byte[] plaintext = "abcdef".getBytes();
        for (int i = 0; i < plaintext.length; i++) {
            out.write(plaintext[i]);
            out.flush();
            int b = in.read();
            String original = new String(plaintext, i, 1);
            String result = new String(new byte[] { (byte)b });
            System.out.println("  " + original + " -> " + result);
        }
    }
}
