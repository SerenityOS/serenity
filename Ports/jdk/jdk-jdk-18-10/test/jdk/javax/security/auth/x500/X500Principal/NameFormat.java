/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4505980 5109882 7049963 7090565
 * @summary X500Principal input name parsing issues and wrong exception thrown
 * @modules java.base/sun.security.x509
 * @run main/othervm -Djava.security.debug=x509,ava NameFormat
 *
 * The debug=ava above must be set in order to check for escaped hex chars.
 */
import javax.security.auth.x500.X500Principal;

public class NameFormat {

    public static void main(String[] args) throws Exception {

        // tests for leading/trailing escaped/non-escaped spaces

        testName("cn=\\ duke   ", "RFC1779", "CN=\" duke\"", 1);
        testName("cn=\\ duke   ", "RFC2253", "CN=\\ duke", 2);
        testName("cn=\\ duke   ", "CANONICAL", "cn=duke", 3);
        testName("cn=\\ duke   ", "toString", "CN=\" duke\"", 4);

        testName("cn= duke", "RFC1779", "CN=duke", 5);
        testName("cn= duke", "RFC2253", "CN=duke", 6);
        testName("cn= duke", "CANONICAL", "cn=duke", 7);
        testName("cn= duke", "toString", "CN=duke", 8);

        testName("cn=duke\\   ", "RFC1779", "CN=\"duke \"", 9);
        testName("cn=duke\\   ", "RFC2253", "CN=duke\\ ", 10);
        testName("cn=duke\\   ", "CANONICAL", "cn=duke", 11);
        testName("cn=duke\\   ", "toString", "CN=\"duke \"", 12);

        testName("cn=duke\\   , ou= sun\\ ", "RFC1779",
                "CN=\"duke \", OU=\"sun \"", 13);
        testName("cn=duke\\   , ou= sun\\ ", "RFC2253",
                "CN=duke\\ ,OU=sun\\ ", 14);
        testName("cn=duke\\   , ou= sun\\ ", "CANONICAL",
                "cn=duke,ou=sun", 15);
        testName("cn=duke\\   , ou= sun\\ ", "toString",
                "CN=\"duke \", OU=\"sun \"", 16);

        // tests for trailing escaped backslash

        testName("cn=duke \\\\\\,test,O=java", "CANONICAL",
                "cn=duke \\\\\\,test,o=java", 17);

        testName("cn=duke\\\\, o=java", "CANONICAL",
                "cn=duke\\\\,o=java", 18);

        X500Principal p = new X500Principal("cn=duke \\\\\\,test,o=java");
        X500Principal p2 = new X500Principal(p.getName("CANONICAL"));
        if (p.getName("CANONICAL").equals(p2.getName("CANONICAL"))) {
            System.out.println("test 19 succeeded");
        } else {
            throw new SecurityException("test 19 failed\n" +
                p.getName("CANONICAL") + " not equal to " +
                p2.getName("CANONICAL"));
        }

        try {
            p = new X500Principal("cn=duke \\\\,test,o=java");
            throw new SecurityException("test 19.5 failed:\n" +
                p.getName("CANONICAL"));
        } catch (IllegalArgumentException iae) {
            System.out.println("test 19.5 succeeded");
            iae.printStackTrace();
        }

        // tests for wrong exception thrown
        try {
            byte[] encoding = {
                (byte)0x17, (byte)0x80, (byte)0x70, (byte)0x41,
                (byte)0x6b, (byte)0x15, (byte)0xdc, (byte)0x84,
                (byte)0xef, (byte)0x58, (byte)0xac, (byte)0x88,
                (byte)0xae, (byte)0xb0, (byte)0x19, (byte)0x7c,
                (byte)0x6f, (byte)0xea, (byte)0xf5, (byte)0x56,
            };
            p = new X500Principal(new java.io.DataInputStream
                (new java.io.ByteArrayInputStream(encoding)));
        } catch (IllegalArgumentException iae) {
            System.out.println("test 20 succeeded");
            iae.printStackTrace();
        } catch (Exception e) {
            System.out.println("test 20 failed");
            throw e;
        }

        // tests for escaping '+' in canonical form

        testName("cn=se\\+an, ou= sun\\ ", "CANONICAL",
                "cn=se\\+an,ou=sun", 21);

        // tests for embedded hex pairs

        testName("CN=Before\\0dAfter,DC=example,DC=net", "toString",
                "CN=Before\\0DAfter, DC=example, DC=net", 22);
        testName("CN=Before\\0dAfter,DC=example,DC=net", "RFC1779",
                "CN=Before\\0DAfter, " +
                "OID.0.9.2342.19200300.100.1.25=example, " +
                "OID.0.9.2342.19200300.100.1.25=net", 23);
        testName("CN=Before\\0dAfter,DC=example,DC=net", "RFC2253",
                "CN=Before\\0DAfter,DC=example,DC=net", 24);
        testName("CN=Before\\0dAfter,DC=example,DC=net", "CANONICAL",
                "cn=before\\0dafter,dc=#16076578616d706c65,dc=#16036e6574", 25);

        testName("CN=Lu\\C4\\8Di\\C4\\87", "toString",
                "CN=Lu\\C4\\8Di\\C4\\87", 26);
        testName("CN=Lu\\C4\\8Di\\C4\\87", "RFC1779",
                "CN=Lu\\C4\\8Di\\C4\\87", 27);
        testName("CN=Lu\\C4\\8Di\\C4\\87", "RFC2253",
                "CN=Lu\\C4\\8Di\\C4\\87", 28);
        testName("CN=Lu\\C4\\8Di\\C4\\87", "CANONICAL",
                "cn=lu\\c4\\8di\\c4\\87", 29);

        try {
            p = new X500Principal("cn=\\gg");
            throw new SecurityException("test 30 failed");
        } catch (IllegalArgumentException iae) {
            System.out.println("test 30 succeeded");
        }

        // tests for invalid escaped chars

        try {
            p = new X500Principal("cn=duke \\test");
            throw new SecurityException("test 31 failed");
        } catch (IllegalArgumentException iae) {
            System.out.println("test 31 succeeded");
        }

        try {
            p = new X500Principal("cn=duke \\?test");
            throw new SecurityException("test 32 failed");
        } catch (IllegalArgumentException iae) {
            System.out.println("test 32 succeeded");
        }

        // tests for X500Name using RFC2253 as format

        try {
            // invalid non-escaped leading space
            sun.security.x509.X500Name name =
                new sun.security.x509.X500Name("cn= duke test", "RFC2253");
            throw new SecurityException("test 33 failed");
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
            System.out.println("test 33 succeeded");
        }

        try {
            // invalid non-escaped trailing space
            sun.security.x509.X500Name name =
                new sun.security.x509.X500Name("cn=duke test ", "RFC2253");
            throw new SecurityException("test 34 failed");
        } catch (java.io.IOException ioe) {
            System.out.println("test 34 succeeded");
        }

        testName("CN=SPECIAL CHARS,OU=\\#\\\"\\,\\<\\>\\+\\;,O=foo, " +
                "L=bar, ST=baz, C=JP", "RFC1779",
                "CN=SPECIAL CHARS, OU=\"#\\\",<>+;\", O=foo, L=bar, " +
                "ST=baz, C=JP", 35);

        // test that double-quoted string is not escaped in RFC 1779 format
        testName("CN=\"\\\"Duke\\\"\"", "RFC1779", "CN=\"Duke\"", 36);
    }

    public static void testName(String in, String outFormat,
                                String expect, int n)
        throws Exception {

        X500Principal p = new X500Principal(in);
        if (outFormat.equalsIgnoreCase("toString")) {
            if (p.toString().equals(expect)) {
                System.out.println("test " + n + " succeeded");
            } else {
                throw new SecurityException("test " + n + " failed:\n" +
                        "expected '" + expect + "'\n" +
                        "got '" + p.toString() + "'");
            }
        } else {
            if (p.getName(outFormat).equals(expect)) {
                System.out.println("test " + n + " succeeded");
            } else {
                throw new SecurityException("test " + n + " failed:\n" +
                        "expected '" + expect + "'\n" +
                        "got '" + p.getName(outFormat) + "'");
            }
        }
    }
}
