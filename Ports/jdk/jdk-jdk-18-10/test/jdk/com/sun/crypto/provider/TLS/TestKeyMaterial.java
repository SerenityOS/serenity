/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6313661
 * @summary Known-answer-test for TlsKeyMaterial generator
 * @author Andreas Sterbenz
 * @modules java.base/sun.security.internal.spec
 */

import java.io.*;
import java.util.*;

import java.security.Security;
import java.security.Provider;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import javax.crypto.spec.*;

import sun.security.internal.spec.*;

public class TestKeyMaterial extends Utils {

    private static int PREFIX_LENGTH = "km-master:  ".length();

    public static void main(String[] args) throws Exception {
        Provider provider = Security.getProvider("SunJCE");

        InputStream in = new FileInputStream(new File(BASE, "keymatdata.txt"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        int n = 0;
        int lineNumber = 0;

        byte[] master = null;
        int major = 0;
        int minor = 0;
        byte[] clientRandom = null;
        byte[] serverRandom = null;
        String cipherAlgorithm = null;
        int keyLength = 0;
        int expandedKeyLength = 0;
        int ivLength = 0;
        int macLength = 0;
        byte[] clientCipherBytes = null;
        byte[] serverCipherBytes = null;
        byte[] clientIv = null;
        byte[] serverIv = null;
        byte[] clientMacBytes = null;
        byte[] serverMacBytes = null;

        while (true) {
            String line = reader.readLine();
            lineNumber++;
            if (line == null) {
                break;
            }
            if (line.startsWith("km-") == false) {
                continue;
            }
            String data = line.substring(PREFIX_LENGTH);
            if (line.startsWith("km-master:")) {
                master = parse(data);
            } else if (line.startsWith("km-major:")) {
                major = Integer.parseInt(data);
            } else if (line.startsWith("km-minor:")) {
                minor = Integer.parseInt(data);
            } else if (line.startsWith("km-crandom:")) {
                clientRandom = parse(data);
            } else if (line.startsWith("km-srandom:")) {
                serverRandom = parse(data);
            } else if (line.startsWith("km-cipalg:")) {
                cipherAlgorithm = data;
            } else if (line.startsWith("km-keylen:")) {
                keyLength = Integer.parseInt(data);
            } else if (line.startsWith("km-explen:")) {
                expandedKeyLength = Integer.parseInt(data);
            } else if (line.startsWith("km-ivlen:")) {
                ivLength = Integer.parseInt(data);
            } else if (line.startsWith("km-maclen:")) {
                macLength = Integer.parseInt(data);
            } else if (line.startsWith("km-ccipkey:")) {
                clientCipherBytes = parse(data);
            } else if (line.startsWith("km-scipkey:")) {
                serverCipherBytes = parse(data);
            } else if (line.startsWith("km-civ:")) {
                clientIv = parse(data);
            } else if (line.startsWith("km-siv:")) {
                serverIv = parse(data);
            } else if (line.startsWith("km-cmackey:")) {
                clientMacBytes = parse(data);
            } else if (line.startsWith("km-smackey:")) {
                serverMacBytes = parse(data);

                System.out.print(".");
                n++;

                KeyGenerator kg =
                    KeyGenerator.getInstance("SunTlsKeyMaterial", provider);
                SecretKey masterKey =
                    new SecretKeySpec(master, "TlsMasterSecret");
                TlsKeyMaterialParameterSpec spec =
                    new TlsKeyMaterialParameterSpec(masterKey, major, minor,
                        clientRandom, serverRandom, cipherAlgorithm,
                        keyLength, expandedKeyLength, ivLength, macLength,
                        null, -1, -1);

                kg.init(spec);
                TlsKeyMaterialSpec result =
                    (TlsKeyMaterialSpec)kg.generateKey();
                match(lineNumber, clientCipherBytes,
                    result.getClientCipherKey());
                match(lineNumber, serverCipherBytes,
                    result.getServerCipherKey());
                match(lineNumber, clientIv, result.getClientIv());
                match(lineNumber, serverIv, result.getServerIv());
                match(lineNumber, clientMacBytes, result.getClientMacKey());
                match(lineNumber, serverMacBytes, result.getServerMacKey());

            } else {
                throw new Exception("Unknown line: " + line);
            }
        }
        if (n == 0) {
            throw new Exception("no tests");
        }
        in.close();
        System.out.println();
        System.out.println("OK: " + n + " tests");
    }

    private static void match(int lineNumber, byte[] out, Object res)
            throws Exception {
        if ((out == null) || (res == null)) {
            if (out != res) {
                throw new Exception("null mismatch line " + lineNumber);
            } else {
                return;
            }
        }
        byte[] b;
        if (res instanceof SecretKey) {
            b = ((SecretKey)res).getEncoded();
        } else if (res instanceof IvParameterSpec) {
            b = ((IvParameterSpec)res).getIV();
        } else {
            throw new Exception(res.getClass().getName());
        }
        if (Arrays.equals(out, b) == false) {
            throw new Exception("mismatch line " + lineNumber);
        }
    }

}
