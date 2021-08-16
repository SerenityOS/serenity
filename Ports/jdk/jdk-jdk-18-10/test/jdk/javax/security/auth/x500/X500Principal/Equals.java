/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4385031
 * @bug 4459925
 * @summary     X500Principal.equals can be optimized,
 *              equals and hashcode are underspecified
 */

import javax.security.auth.x500.X500Principal;

public class Equals {

    private static final String p1String =
            "O=sun, Ou=eng,cn=Test 1, EMAILADDRESS=duke@sun.com, UID=1   ";

    private static final String p2String =
            " o=SUN,OU=eng,  cn=test  1,emailaddress = duke@sun.com,UID=1";

    private static final String p3String =
            " o   = SUN,  cn=test  1+  emailaddress = duke@sun.com +  uId =5";

    private static final String p4String =
            "o=SUN,uid=5 + emailaddress = duke@sun.com  +cn=test 1";

    private static final String p5String =
            "emailaddress = duke@sun.com +  SURNAME=blah";

    private static final String p6String =
            "surname=blah+ emailAddress =    duke@sun.com  ";

    private static final String p7String =
            "o=sun, ou=esc\\\"quote, cn=duke";

    private static final String p8String =
            "o=sun, ou=   esc\\\"quote,cn=duke";

    public static void main(String[] args) {

        // test regular equals
        X500Principal p1 = new X500Principal(p1String);
        X500Principal p2 = new X500Principal(p2String);

        printName("Principal 1:", p1String, p1);
        printName("Principal 2:", p2String, p2);

        if (!p1.equals(p2))
            throw new SecurityException("Equals test failed: #1");

        X500Principal notEqual = new X500Principal("cn=test2");
        if (p1.equals(notEqual))
            throw new SecurityException("Equals test failed: #2");

        if (p1.equals(null))
            throw new SecurityException("Equals test failed: #3");

        if (p1.hashCode() != p2.hashCode())
            throw new SecurityException("Equals test failed: #4");

        // test multiple AVA's in an RDN
        X500Principal p3 = new X500Principal(p3String);
        X500Principal p4 = new X500Principal(p4String);

        printName("Principal 3:", p3String, p3);
        printName("Principal 4:", p4String, p4);

        if (!p3.equals(p4))
            throw new SecurityException("Equals test failed: #5");

        if (p1.equals(p3) || p2.equals(p3))
            throw new SecurityException("Equals test failed: #6");

        if (p3.hashCode() != p4.hashCode())
            throw new SecurityException("Equals test failed: #7");

        X500Principal p5 = new X500Principal(p5String);
        X500Principal p6 = new X500Principal(p6String);

        printName("Principal 5:", p5String, p5);
        printName("Principal 6:", p6String, p6);

        if (!p5.equals(p6))
            throw new SecurityException("Equals test failed: #8");

        if (p5.hashCode() != p6.hashCode())
            throw new SecurityException("Equals test failed: #9");

        X500Principal p7 = new X500Principal(p7String);
        X500Principal p8 = new X500Principal(p8String);

        printName("Principal 7:", p7String, p7);
        printName("Principal 8:", p8String, p8);

        if (!p7.equals(p8))
            throw new SecurityException("Equals test failed: #10");

        if (p7.hashCode() != p8.hashCode())
            throw new SecurityException("Equals test failed: #11");

        System.out.println("Equals test passed");
    }

    static void printName(String heading, String input, X500Principal p) {
        System.out.println(heading);
        System.out.println(" input:\t\t" + input);
        System.out.println();
        System.out.println(" toString:\t" + p.toString());
        System.out.println();
        System.out.println(" getName:\t" + p.getName());
        System.out.println();
        System.out.println(" getName 1779:\t" + p.getName("rfc1779"));
        System.out.println();
        System.out.println(" getName 2253:\t" + p.getName("rfc2253"));
        System.out.println();
        System.out.println(" getName canon:\t" + p.getName("canonical"));
        System.out.println();
    }
}
