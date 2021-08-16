/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4894151 7055362
 * @summary known answer test for OAEP encryption
 * @author Andreas Sterbenz
 */

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.*;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;

/**
 * Known answer test for OAEP encryption. The "oaep-vect.txt" file was taken
 * from the RSA Security web site. It contains a number of test cases using
 * keys of various lengths.
 *
 * Note that we only test decryption. We cannot do a KAT encryption test
 * because our APIs do now allow us to explicitly specify the seed.
 * Encryption is tested in a different test case.
 */
public class TestOAEP_KAT {

    private final static String BASE = System.getProperty("test.src", ".");

    private static BigInteger n, e, d, p, q, pe, qe, coeff;

    private static byte[] plainText, seed, cipherText, cipherText2;

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        Provider provider = Security.getProvider("SunJCE");
        Provider kfProvider = Security.getProvider("SunRsaSign");
        System.out.println("Testing provider " + provider.getName() + "...");
        Cipher c = Cipher.getInstance("RSA/ECB/OAEPwithSHA1andMGF1Padding", provider);
        KeyFactory kf = KeyFactory.getInstance("RSA", kfProvider);
        try (InputStream in = new FileInputStream(new File(BASE, "oaep-vect.txt"));
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(in, "UTF8"))) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.equals("# RSA modulus n:")) {
                    n = parseNumber(reader);
                } else if (line.equals("# RSA public exponent e:")) {
                    e = parseNumber(reader);
                } else if (line.equals("# RSA private exponent d:")) {
                    d = parseNumber(reader);
                } else if (line.equals("# Prime p:")) {
                    p = parseNumber(reader);
                } else if (line.equals("# Prime q:")) {
                    q = parseNumber(reader);
                } else if (line.equals("# p's CRT exponent dP:")) {
                    pe = parseNumber(reader);
                } else if (line.equals("# q's CRT exponent dQ:")) {
                    qe = parseNumber(reader);
                } else if (line.equals("# CRT coefficient qInv:")) {
                    coeff = parseNumber(reader);
                } else if (line.equals("# Message to be encrypted:")) {
                    plainText = parseBytes(reader);
                } else if (line.equals("# Seed:")) {
                    seed = parseBytes(reader);
                } else if (line.equals("# Encryption:")) {
                    cipherText = parseBytes(reader);
                    // do encryption test first
                    KeySpec pubSpec = new RSAPublicKeySpec(n, e);
                    PublicKey pubKey = kf.generatePublic(pubSpec);
                    c.init(Cipher.ENCRYPT_MODE, pubKey, new MyRandom(seed));
                    cipherText2 = c.doFinal(plainText);
                    if (Arrays.equals(cipherText2, cipherText) == false) {
                        throw new Exception("Encryption mismatch");
                    }
                    // followed by decryption test
                    KeySpec privSpec = new RSAPrivateCrtKeySpec(n, e, d, p, q, pe, qe, coeff);
                    PrivateKey privKey = kf.generatePrivate(privSpec);
                    c.init(Cipher.DECRYPT_MODE, privKey);
                    byte[] dec = c.doFinal(cipherText);
                    if (Arrays.equals(plainText, dec) == false) {
                        throw new Exception("Decryption mismatch");
                    }
                } else if (line.startsWith("# ------------------------------")) {
                    // ignore, do not print
                } else {
                    // unknown line (comment), print
                    System.out.println(": " + line);
                }
            }
        }
        long stop = System.currentTimeMillis();
        System.out.println("Done (" + (stop - start) + " ms).");
    }

    private static BigInteger parseNumber(BufferedReader reader) throws IOException {
        return new BigInteger(1, parseBytes(reader));
    }

    private static byte[] parseBytes(BufferedReader reader) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                throw new EOFException("Unexpected EOF");
            }
            line = line.trim();
            if (line.length() == 0) {
                break;
            }
            buffer.write(parse(line));
        }
        return buffer.toByteArray();
    }

    public static byte[] parse(String s) {
        try {
            int n = s.length();
            ByteArrayOutputStream out = new ByteArrayOutputStream(n / 3);
            StringReader r = new StringReader(s);
            while (true) {
                int b1 = nextNibble(r);
                if (b1 < 0) {
                    break;
                }
                int b2 = nextNibble(r);
                if (b2 < 0) {
                    throw new RuntimeException("Invalid string " + s);
                }
                int b = (b1 << 4) | b2;
                out.write(b);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] b(String s) {
        return parse(s);
    }

    private static int nextNibble(StringReader r) throws IOException {
        while (true) {
            int ch = r.read();
            if (ch == -1) {
                return -1;
            } else if ((ch >= '0') && (ch <= '9')) {
                return ch - '0';
            } else if ((ch >= 'a') && (ch <= 'f')) {
                return ch - 'a' + 10;
            } else if ((ch >= 'A') && (ch <= 'F')) {
                return ch - 'A' + 10;
            }
        }
    }

}

class MyRandom extends SecureRandom {

    private byte[] source;
    private int count;

    MyRandom(byte[] source) {
        this.source = (byte[]) source.clone();
        count = 0;
    }

    public void nextBytes(byte[] bytes) {
        if (bytes.length > source.length - count) {
            throw new RuntimeException("Insufficient random data");
        }
        System.arraycopy(source, count, bytes, 0, bytes.length);
        count += bytes.length;
    }
}
