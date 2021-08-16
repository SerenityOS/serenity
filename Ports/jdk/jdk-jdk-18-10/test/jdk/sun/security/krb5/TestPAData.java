/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6570062
 * @summary Kerberos authentication regression
 * @modules java.base/sun.security.util
 *          java.security.jgss/sun.security.krb5.internal
 */

import sun.security.krb5.internal.KRBError;
import sun.security.krb5.internal.Krb5;
import sun.security.util.DerValue;

public class TestPAData {
    public static void main(String[] args) throws Exception {
        // This is the dump of a KRB-ERROR data, no sensitive info included.
        byte[] bytes = {
            (byte)0X7E, (byte)0X71, (byte)0X30, (byte)0X6F,
            (byte)0XA0, (byte)0X03, (byte)0X02, (byte)0X01,
            (byte)0X05, (byte)0XA1, (byte)0X03, (byte)0X02,
            (byte)0X01, (byte)0X1E, (byte)0XA4, (byte)0X11,
            (byte)0X18, (byte)0X0F, (byte)0X32, (byte)0X30,
            (byte)0X30, (byte)0X37, (byte)0X30, (byte)0X36,
            (byte)0X32, (byte)0X31, (byte)0X32, (byte)0X31,
            (byte)0X30, (byte)0X32, (byte)0X34, (byte)0X33,
            (byte)0X5A, (byte)0XA5, (byte)0X05, (byte)0X02,
            (byte)0X03, (byte)0X0A, (byte)0XC8, (byte)0XC5,
            (byte)0XA6, (byte)0X03, (byte)0X02, (byte)0X01,
            (byte)0X12, /* The errorcode at bytes[44] */
                        (byte)0XA9, (byte)0X0A, (byte)0X1B,
            (byte)0X08, (byte)0X4E, (byte)0X33, (byte)0X2E,
            (byte)0X4C, (byte)0X4F, (byte)0X43, (byte)0X41,
            (byte)0X4C, (byte)0XAA, (byte)0X1D, (byte)0X30,
            (byte)0X1B, (byte)0XA0, (byte)0X03, (byte)0X02,
            (byte)0X01, (byte)0X02, (byte)0XA1, (byte)0X14,
            (byte)0X30, (byte)0X12, (byte)0X1B, (byte)0X06,
            (byte)0X6B, (byte)0X72, (byte)0X62, (byte)0X74,
            (byte)0X67, (byte)0X74, (byte)0X1B, (byte)0X08,
            (byte)0X4E, (byte)0X33, (byte)0X2E, (byte)0X4C,
            (byte)0X4F, (byte)0X43, (byte)0X41, (byte)0X4C,
            (byte)0XAC, (byte)0X19, (byte)0X04, (byte)0X17,
            (byte)0X30, (byte)0X15, (byte)0XA1, (byte)0X03,
            (byte)0X02, (byte)0X01, (byte)0X03, (byte)0XA2,
            (byte)0X0E, (byte)0X04, (byte)0X0C, (byte)0X72,
            (byte)0X00, (byte)0X00, (byte)0XC0, (byte)0X00,
            (byte)0X00, (byte)0X00, (byte)0X00, (byte)0X01,
            (byte)0X00, (byte)0X00, (byte)0X00
        };
        String err = "";

        try {
            new KRBError(new DerValue(bytes));
        } catch (Exception e) {
            err += "Test 1 fails.\n";
        }

        try {
            bytes[44] = Krb5.KDC_ERR_PREAUTH_REQUIRED;
            new KRBError(new DerValue(bytes));
            err += "Test 2 fails.\n";
        } catch (Exception e) {
            // correct bahavior
        }
        if (err != "") {
            throw new Exception(err);
        }
    }
}
