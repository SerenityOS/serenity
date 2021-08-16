/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4635618
 * @summary Support for manipulating LDAP Names
 */

import javax.naming.ldap.*;
import java.util.ArrayList;
import java.util.List;
import javax.naming.InvalidNameException;

/**
 * Tests for LDAP name parsing
 */

public class LdapParserTests {
    public static void main(String args[])
                throws Exception {
        Rdn rdn = null;

        /**
         * Presence of any of these characters in an attribute value
         * without a preceeding escape is considered Illegal by
         * the LDAP parser.
         */
        String[] mustEscSpecials = new String[]
                                {";", ",", "\\", "+"};

        /**
         * The special characters that should be preceeded by an escape
         */
        String[] specials = new String[]
                        {";", ",", "\\", "<", ">", "#", "\"", "+"};

        /**
         * Test with unescaped speicial characters in the Rdn
         */
        System.out.println();
        System.out.print("*****Tests with unescaped special ");
        System.out.println("characters in the Rdn*****");

        for (int i = 0; i < mustEscSpecials.length; i++) {
            String rdnStr = "cn=Juicy" + mustEscSpecials[i] + "Fruit";
            testInvalids(rdnStr);
        }

        /*
         * special characters with a preceeding backslash must be accepted.
         */
        System.out.println();
        System.out.println("******Special character escaping tests ******");
        for (int i = 0; i < specials.length; i++) {
            rdn = new Rdn("cn=Juicy\\" + specials[i] + "Fruit");
        }
        System.out.println("Escape leading space:" +
                new Rdn("cn=\\  Juicy Fruit")); // escaped leading space
        System.out.println("Escaped leading #:" +
                new Rdn("cn=\\#Juicy Fruit"));  // escaped leading # in string
        System.out.println("Escaped trailing space:" +
                new Rdn("cn=Juicy Fruit\\  ")); // escaped trailing space

        // Unescaped special characters at the beginning of a value
        System.out.println();
        System.out.println(
                "******Other unescaped special character tests ******");
        rdn = new Rdn("cn=  Juicy Fruit");
        System.out.println(
            "Accepted Rdn with value containing leading spaces:" +
            rdn.toString());
        rdn = new Rdn("cn=Juicy Fruit  ");
        System.out.println(
            "Accepted Rdn with value containing trailing spaces:" +
            rdn.toString());

        String[] invalids =  new String[]
                {"cn=#xabc", "cn=#axcd", "cn=#abcx", "cn=#abcdex"};

        for (int i = 0; i < invalids.length; i++) {
            testInvalids(invalids[i]);
        }

        /**
         * Other special cases
         */
        System.out.println();
        System.out.println(
                "***************Other special cases****************");

        LdapName name = new LdapName("");
        System.out.println("Empty LDAP name:" + name.toString());

        // Rdn with quoted value
        rdn = new Rdn("cn=\"Juicy ,=+<>#; Fruit\"");
        System.out.println("Quoted Rdn string:" + rdn.toString());

        // Rdn with unicode value
        rdn = new Rdn("SN=Lu\\C4\\8Di\\C4\\87");
        System.out.println("Unicode Rdn string:" + rdn.toString());

        /*
         * oid type and binary value
         */
        name = new LdapName(
                "1.3.6.1.4.1.466.0=#04024869,O=Test,C=GB");
        System.out.println("binary valued LDAP name:" + name.toString());

        // ';' seperated name- RFC 1779 style
        name = new LdapName("CN=Steve Kille;O=Isode;C=GB");
        System.out.println("';' seperated LDAP name:" + name.toString());
    }

    static void testInvalids(String rdnStr) throws Exception {
        boolean isExcepThrown = false;
        Rdn rdn = null;
        try {
            rdn = new Rdn(rdnStr);
        } catch (InvalidNameException e) {
            System.out.println("Caught the right exception: " + e);
            isExcepThrown = true;
        } catch (IllegalArgumentException e) {
            System.out.println("Caught the right exception: " + e);
            isExcepThrown = true;
        }
        if (!isExcepThrown) {
            throw new Exception(
                    "Accepted an invalid Rdn string:" +
                    rdnStr + " as Rdn: " + rdn);
        }
    }
}
