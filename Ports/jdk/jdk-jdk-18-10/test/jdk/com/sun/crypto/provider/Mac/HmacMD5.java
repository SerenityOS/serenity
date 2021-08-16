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
 * @summary Checks compliance of the HMAC-MD5 implementation with
 *      RFC 2104, using the test vectors specified there.
 * @author Jan Luehe
 */

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class HmacMD5 {

    public static void main(String argv[]) throws Exception {
            int i, j, n;
            Mac mac;

            byte[][][] test_data = {
                {
                    { (byte)0x0b, (byte)0x0b, (byte)0x0b, (byte)0x0b,
                      (byte)0x0b, (byte)0x0b, (byte)0x0b, (byte)0x0b,
                      (byte)0x0b, (byte)0x0b, (byte)0x0b, (byte)0x0b,
                      (byte)0x0b, (byte)0x0b, (byte)0x0b, (byte)0x0b }, // key1
                    "Hi There".getBytes(), // data1
                    { (byte)0x92, (byte)0x94, (byte)0x72, (byte)0x7a,
                      (byte)0x36, (byte)0x38, (byte)0xbb, (byte)0x1c,
                      (byte)0x13, (byte)0xf4, (byte)0x8e, (byte)0xf8,
                      (byte)0x15, (byte)0x8b, (byte)0xfc, (byte)0x9d // result1
                    }
                },
                {
                    "Jefe".getBytes(), // key2
                    "what do ya want for nothing?".getBytes(), // data2
                    { (byte)0x75, (byte)0x0c, (byte)0x78, (byte)0x3e,
                      (byte)0x6a, (byte)0xb0, (byte)0xb5, (byte)0x03,
                      (byte)0xea, (byte)0xa8, (byte)0x6e, (byte)0x31,
                      (byte)0x0a, (byte)0x5d, (byte)0xb7, (byte)0x38 // result2
                    }
                },
                {
                    { (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA,
                      (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA,
                      (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA,
                      (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA // key3
                    },
                    { (byte)0xDD, (byte)0xDD, (byte)0xDD, (byte)0xDD,
                      (byte)0xDD, (byte)0xDD, (byte)0xDD, (byte)0xDD,
                      (byte)0xDD, (byte)0xDD, (byte)0xDD, (byte)0xDD,
                      (byte)0xDD, (byte)0xDD, (byte)0xDD, (byte)0xDD,
                      (byte)0xDD, (byte)0xDD, (byte)0xDD, (byte)0xDD,
                      (byte)0xDD, (byte)0xDD, (byte)0xDD, (byte)0xDD,
                      (byte)0xDD, (byte)0xDD, (byte)0xDD, (byte)0xDD,
                      (byte)0xDD, (byte)0xDD, (byte)0xDD, (byte)0xDD,
                      (byte)0xDD, (byte)0xDD, (byte)0xDD, (byte)0xDD,
                      (byte)0xDD, (byte)0xDD, (byte)0xDD, (byte)0xDD,
                      (byte)0xDD, (byte)0xDD, (byte)0xDD, (byte)0xDD,
                      (byte)0xDD, (byte)0xDD, (byte)0xDD, (byte)0xDD,
                      (byte)0xDD, (byte)0xDD // data3
                    },
                    { (byte)0x56, (byte)0xbe, (byte)0x34, (byte)0x52,
                      (byte)0x1d, (byte)0x14, (byte)0x4c, (byte)0x88,
                      (byte)0xdb, (byte)0xb8, (byte)0xc7, (byte)0x33,
                      (byte)0xf0, (byte)0xe8, (byte)0xb3, (byte)0xf6 // result3
                    }
                }
            };

            mac = Mac.getInstance("HmacMD5", "SunJCE");
            for (i=0; i<3; i++) {
                j=0;

                mac.init(new SecretKeySpec(test_data[i][j++], "HMAC"));
                byte[] result = mac.doFinal(test_data[i][j++]);
                if (result.length != test_data[i][j].length) {
                    throw new Exception("Different result length");
                }
                for (n=0; n<result.length; n++) {
                    if (result[n] != test_data[i][j][n]) {
                        throw new Exception("Different");
                    }
                }
            }

            // now test multiple-part operation, using the 2nd test vector
            mac = Mac.getInstance("HmacMD5", "SunJCE");
            mac.init(new SecretKeySpec("Jefe".getBytes(), "HMAC"));
            mac.update("what do ya ".getBytes());
            mac.update("want for ".getBytes());
            mac.update("nothing?".getBytes());
            byte[] result = mac.doFinal();
            if (result.length != test_data[1][2].length) {
                throw new Exception("Different result length");
            }
            for (i=0; i<result.length; i++) {
                if (result[i] != test_data[1][2][i]) {
                    throw new Exception("Different");
                }
            }

            System.out.println("Test SUCCEEDED");
    }
}
