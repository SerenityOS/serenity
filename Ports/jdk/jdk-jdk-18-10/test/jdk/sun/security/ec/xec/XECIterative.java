/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8171277
 * @summary XEC curve operations iterative test vectors
 * @library /test/lib
 * @build jdk.test.lib.Convert
 * @modules jdk.crypto.ec/sun.security.ec
 * @run main XECIterative 0 10000
 * @run main XECIterative 10000 20000
 * @run main XECIterative 20000 30000
 * @run main XECIterative 30000 40000
 * @run main XECIterative 40000 50000
 * @run main XECIterative 50000 60000
 * @run main XECIterative 60000 70000
 * @run main XECIterative 70000 80000
 * @run main XECIterative 80000 90000
 * @run main XECIterative 90000 100000
 */

import sun.security.ec.*;

import java.io.*;
import java.security.spec.NamedParameterSpec;
import java.util.*;

/*
 * This test is derived from the iterative test in RFC 7748. To produce the
 * test vectors, the implementation ran for 1,000,000 iterations and saved
 * values of k and u at periodic checkpoints. The RFC includes the correct
 * value of k after 1,000,000 iterations, and this value was used to ensure
 * that the test vectors are correct. This test has multiple @run tags so that
 * no single run takes too long.
 */

public class XECIterative {

    private static class KU {

        public byte[] k;
        public byte[] u;

    }

    public static void main(String[] args) throws IOException {

        long start = Long.parseLong(args[0]);
        long end = Long.parseLong(args[1]);

        XECIterative m = new XECIterative();

        m.runIterativeTest("X25519", start, end);
        m.runIterativeTest("X448", start, end);

    }

    private void runIterativeTest(String opName, long start, long end)
        throws IOException {

        NamedParameterSpec paramSpec = new NamedParameterSpec(opName);
        XECParameters settings =
            XECParameters.get(RuntimeException::new, paramSpec);
        XECOperations ops = new XECOperations(settings);

        File vectorFile = new File(System.getProperty("test.src", "."),
        opName + ".iter");

        Map<Long, KU> testIters = new HashMap<Long, KU>();
        BufferedReader in = new BufferedReader(new FileReader(vectorFile));
        String line;
        while ((line = in.readLine()) != null) {
            StringTokenizer tok = new StringTokenizer(line, ",");
            long iter = Long.parseLong(tok.nextToken());
            String kOrU = tok.nextToken();
            byte[] value = HexFormat.of().parseHex(tok.nextToken());
            KU entry = testIters.get(iter);
            if (entry == null) {
                entry = new KU();
                testIters.put(iter, entry);
            }
            if (kOrU.equals("k")) {
                entry.k = value;
            } else {
                entry.u = value;
            }
        }

        KU startEntry = testIters.get(start);
        byte[] k = startEntry.k.clone();
        byte[] u = startEntry.u.clone();

        for (long i = start; i <= end; i++) {
            KU curEntry;
            if (i % 1000 == 0 && (curEntry = testIters.get(i)) != null) {
                if (!Arrays.equals(k, curEntry.k)) {
                    throw new RuntimeException("At iter " + i + ": expected k: "
                        + HexFormat.of().withUpperCase().formatHex(curEntry.k)
                        + ", computed k: " + HexFormat.of().withUpperCase().formatHex(k));
                }
                if (!Arrays.equals(u, curEntry.u)) {
                    throw new RuntimeException("At iter " + i + ": expected u: "
                     + HexFormat.of().withUpperCase().formatHex(curEntry.u)
                     + ", computed u: " + HexFormat.of().withUpperCase().formatHex(u));
                }
                System.out.println(opName + " checkpoint passed at " + i);
            }

            byte[] k_copy = Arrays.copyOf(k, k.length);
            byte[] u_out = ops.encodedPointMultiply(k, u);
            u = k_copy;
            k = u_out;
        }
    }

}


