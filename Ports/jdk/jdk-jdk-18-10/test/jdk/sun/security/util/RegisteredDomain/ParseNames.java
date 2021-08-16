/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8228969 8244087
 * @modules java.base/sun.security.util
 * @summary unit test for RegisteredDomain
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.util.Objects;
import java.util.Optional;
import sun.security.util.RegisteredDomain;

public class ParseNames {

    public static void main(String[] args) throws Exception {
        String dir = System.getProperty("test.src", ".");
        File f = new File(dir, "tests.dat");
        try (FileInputStream fis = new FileInputStream(f)) {
            InputStreamReader r = new InputStreamReader(fis, "UTF-8");
            BufferedReader reader = new BufferedReader(r);

            String s;
            int linenumber = 0;
            boolean allTestsPass = true;

            while ((s = reader.readLine()) != null) {
                linenumber++;
                if ("".equals(s) || s.charAt(0) == '#') {
                    continue;
                }
                String[] tokens = s.split("\\s+");
                if (tokens.length != 3) {
                    throw new Exception(
                        String.format("Line %d: test data format incorrect",
                                      linenumber));
                }
                if (tokens[1].equals("null")) {
                    tokens[1] = null;
                }
                if (tokens[2].equals("null")) {
                    tokens[2] = null;
                }
                allTestsPass &= runTest(linenumber, tokens[0],
                                        tokens[1], tokens[2]);
            }
            if (allTestsPass) {
                System.out.println("Test passed.");
            } else {
                throw new Exception("Test failed.");
            }
        }
    }

    private static boolean runTest(int lnum, String target,
                                   String expPubSuffix, String expRegDomain) {

        System.out.println("target:" + target);
        Optional<RegisteredDomain> rd = RegisteredDomain.from(target);
        String regName = rd.map(RegisteredDomain::name).orElse(null);
        if (!Objects.equals(expRegDomain, regName)) {
            System.out.printf(
                "Line %d: %s, Expected registered domain: %s, Got: %s\n",
                lnum, target, expRegDomain, regName);
            return false;
        }

        if (expRegDomain == null) {
            return true;
        }

        String pubSuffix = rd.map(RegisteredDomain::publicSuffix).orElse(null);
        if (!Objects.equals(expPubSuffix, pubSuffix)) {
            System.out.printf(
                "Line %d: %s, Expected public suffix: %s, Got: %s\n",
                lnum, target, expPubSuffix, pubSuffix);
            return false;
        }

        return true;
    }
}
