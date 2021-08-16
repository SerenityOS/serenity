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
 * @summary Basic known-answer-test for TlsPrf
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

public class TestPRF extends Utils {

    private static int PREFIX_LENGTH = "prf-output: ".length();

    public static void main(String[] args) throws Exception {
        Provider provider = Security.getProvider("SunJCE");

        InputStream in = new FileInputStream(new File(BASE, "prfdata.txt"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        int n = 0;
        int lineNumber = 0;

        byte[] secret = null;
        String label = null;
        byte[] seed = null;
        int length = 0;
        byte[] output = null;

        while (true) {
            String line = reader.readLine();
            lineNumber++;
            if (line == null) {
                break;
            }
            if (line.startsWith("prf-") == false) {
                continue;
            }

            String data = line.substring(PREFIX_LENGTH);
            if (line.startsWith("prf-secret:")) {
                secret = parse(data);
            } else if (line.startsWith("prf-label:")) {
                label = data;
            } else if (line.startsWith("prf-seed:")) {
                seed = parse(data);
            } else if (line.startsWith("prf-length:")) {
                length = Integer.parseInt(data);
            } else if (line.startsWith("prf-output:")) {
                output = parse(data);

                System.out.print(".");
                n++;

                KeyGenerator kg =
                    KeyGenerator.getInstance("SunTlsPrf", provider);
                SecretKey inKey;
                if (secret == null) {
                    inKey = null;
                } else {
                    inKey = new SecretKeySpec(secret, "Generic");
                }
                TlsPrfParameterSpec spec =
                    new TlsPrfParameterSpec(inKey, label, seed, length,
                        null, -1, -1);
                kg.init(spec);
                SecretKey key = kg.generateKey();
                byte[] enc = key.getEncoded();
                if (Arrays.equals(output, enc) == false) {
                    throw new Exception("mismatch line: " + lineNumber);
                }
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

}
