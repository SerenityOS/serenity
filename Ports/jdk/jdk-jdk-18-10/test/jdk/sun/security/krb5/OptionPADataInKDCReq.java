/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6648972
 * @summary KDCReq.init always read padata
 * @modules java.base/sun.security.util
 *          java.security.jgss/sun.security.krb5.internal
 */
import sun.security.krb5.internal.ETypeInfo2;
import sun.security.krb5.internal.KDCReq;
import sun.security.util.DerValue;

public class OptionPADataInKDCReq {
    public static void main(String[] args) throws Exception {
        /*
         * This is a AS-REQ block without padata. The content is --
        [APPLICATION 10] SEQUENCE {
            [1] INTEGER 5
            [2] INTEGER 10
            [4] SEQUENCE {
                [0] BIT STRING 01000000 10000001 00000000 00010000
                [1] SEQUENCE {
                    [0] INTEGER 1
                    [1] SEQUENCE {
                        STRING administrator
                    }
                }
                [2] STRING N3
                [3] SEQUENCE {
                    [0] INTEGER 2
                    [1] SEQUENCE {
                        STRING krbtgt
                        STRING N3
                    }
                }
                [5] TIME Sun Sep 13 10:48:05 CST 2037
                [6] TIME Sun Sep 13 10:48:05 CST 2037
                [7] INTEGER 2101281516
                [8] SEQUENCE {
                    INTEGER 23
                    INTEGER -133
                    INTEGER -128
                    INTEGER 3
                    INTEGER 1
                    INTEGER 24
                    INTEGER -135
                }
                [9] SEQUENCE {
                    SEQUENCE {
                        [0] INTEGER 20
                        [1] OCTET STRING
                            0000: 58 50 20 20 20 20 20 20   20 20 20 20 20 20 20 20  XP
                    }
                }
            }
        }
        */
        byte[] b = {
            (byte)0x6a, (byte)0x81, (byte)0xbf, (byte)0x30, (byte)0x81, (byte)0xbc, (byte)0xa1, (byte)0x03,
            (byte)0x02, (byte)0x01, (byte)0x05, (byte)0xa2, (byte)0x03, (byte)0x02, (byte)0x01, (byte)0x0a,
            (byte)0xa4, (byte)0x81, (byte)0xaf, (byte)0x30, (byte)0x81, (byte)0xac, (byte)0xa0, (byte)0x07,
            (byte)0x03, (byte)0x05, (byte)0x00, (byte)0x40, (byte)0x81, (byte)0x00, (byte)0x10, (byte)0xa1,
            (byte)0x1a, (byte)0x30, (byte)0x18, (byte)0xa0, (byte)0x03, (byte)0x02, (byte)0x01, (byte)0x01,
            (byte)0xa1, (byte)0x11, (byte)0x30, (byte)0x0f, (byte)0x1b, (byte)0x0d, (byte)0x61, (byte)0x64,
            (byte)0x6d, (byte)0x69, (byte)0x6e, (byte)0x69, (byte)0x73, (byte)0x74, (byte)0x72, (byte)0x61,
            (byte)0x74, (byte)0x6f, (byte)0x72, (byte)0xa2, (byte)0x04, (byte)0x1b, (byte)0x02, (byte)0x4e,
            (byte)0x33, (byte)0xa3, (byte)0x17, (byte)0x30, (byte)0x15, (byte)0xa0, (byte)0x03, (byte)0x02,
            (byte)0x01, (byte)0x02, (byte)0xa1, (byte)0x0e, (byte)0x30, (byte)0x0c, (byte)0x1b, (byte)0x06,
            (byte)0x6b, (byte)0x72, (byte)0x62, (byte)0x74, (byte)0x67, (byte)0x74, (byte)0x1b, (byte)0x02,
            (byte)0x4e, (byte)0x33, (byte)0xa5, (byte)0x11, (byte)0x18, (byte)0x0f, (byte)0x32, (byte)0x30,
            (byte)0x33, (byte)0x37, (byte)0x30, (byte)0x39, (byte)0x31, (byte)0x33, (byte)0x30, (byte)0x32,
            (byte)0x34, (byte)0x38, (byte)0x30, (byte)0x35, (byte)0x5a, (byte)0xa6, (byte)0x11, (byte)0x18,
            (byte)0x0f, (byte)0x32, (byte)0x30, (byte)0x33, (byte)0x37, (byte)0x30, (byte)0x39, (byte)0x31,
            (byte)0x33, (byte)0x30, (byte)0x32, (byte)0x34, (byte)0x38, (byte)0x30, (byte)0x35, (byte)0x5a,
            (byte)0xa7, (byte)0x06, (byte)0x02, (byte)0x04, (byte)0x7d, (byte)0x3f, (byte)0x02, (byte)0xec,
            (byte)0xa8, (byte)0x19, (byte)0x30, (byte)0x17, (byte)0x02, (byte)0x01, (byte)0x17, (byte)0x02,
            (byte)0x02, (byte)0xff, (byte)0x7b, (byte)0x02, (byte)0x01, (byte)0x80, (byte)0x02, (byte)0x01,
            (byte)0x03, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x01, (byte)0x18, (byte)0x02,
            (byte)0x02, (byte)0xff, (byte)0x79, (byte)0xa9, (byte)0x1d, (byte)0x30, (byte)0x1b, (byte)0x30,
            (byte)0x19, (byte)0xa0, (byte)0x03, (byte)0x02, (byte)0x01, (byte)0x14, (byte)0xa1, (byte)0x12,
            (byte)0x04, (byte)0x10, (byte)0x58, (byte)0x50, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20,
        };
        new KDCReq(b, 0x0a);

        /*
         * This is a fake ETYPEINFO2 block with no salt
            SEQUENCE {
                [0] INTEGER 0
                [2] OCTET STRING 0000: 00                                                 .
            }
         */
        byte[] b2 = {
            (byte)0x30, (byte)0x0a, (byte)0xa0, (byte)0x03, (byte)0x02, (byte)0x01, (byte)0x00, (byte)0xa2,
            (byte)0x03, (byte)0x04, (byte)0x01, (byte)0x00,
        };

        ETypeInfo2 e2 = new ETypeInfo2(new DerValue(b2));
        if (e2.getSalt() != null || e2.getParams() == null) {
            throw new Exception("ETypeInfo2 decoding error");
        }
    }
}
