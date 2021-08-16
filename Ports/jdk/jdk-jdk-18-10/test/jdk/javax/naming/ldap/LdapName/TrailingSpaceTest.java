/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4985339
 * @summary javax.naming.ldap.LdapName(String) doesn't parse
 *           some strings well
 */

import javax.naming.ldap.*;
import javax.naming.*;

public class TrailingSpaceTest {

    public static void main(String[] args) throws Exception {
        String[] input = {"cn=Tyler\\ ",
                        "cn=Ty ler",
                        "cn=Tyler\\\\  ",
                        "cn=Tyler\\\\\\ ",
                        "cn=   Tyler     ",
                        "cn=Tyler\\\\ \\ ",
                        "cn= ",
                        "cn=  \\     "
                    };

        String[] expected = { "Tyler ",
                                "Ty ler",
                                "Tyler\\",
                                "Tyler\\ ",
                                "Tyler",
                                "Tyler\\  ",
                                "",
                                " "
                            };

        try {
            System.out.println("*************************");
            System.out.println();

            for (int i = 0; i < input.length; i++) {

                Rdn rdn = new Rdn(input[i]);

                System.out.println((i + 1) + ") RDN string: [" +
                                        input[i] + "]");
                Object value = rdn.getValue();

                // escape the value
                String escaped = Rdn.escapeValue(value);
                System.out.println("escaped: [" + escaped + "]");

                // unescape the value
                String unescaped = (String) Rdn.unescapeValue(escaped);
                System.out.println("unescaped: [" + unescaped + "]");

                System.out.println();
                System.out.println("*************************");
                System.out.println();

                if (!unescaped.equals(expected[i])) {
                   throw new Exception("Invalid unescaping for: " +
                                        " input #" + (i + 1));
                }
            }
        } catch (InvalidNameException e) {
            e.printStackTrace();
        }
    }
}
