/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4635618 7059542
 * @summary Support for manipulating LDAP Names
 *          JNDI name operations should be locale independent
 */

import javax.naming.ldap.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import javax.naming.InvalidNameException;

/**
 * Tests for LdapName/Rdn compareTo, equals and hashCode methods.
 */
public class CompareToEqualsTests {

    public static void main(String args[])
                throws Exception {
         Locale reservedLocale = Locale.getDefault();
         try {
            /**
             * Test cases:
             * 1) Same RDNs.
             * 2) same RDN sequence with an AVA ordered differently.
             * 3) RDN sequences of a differing AVA.
             * 4) RDN sequence of different length.
             * 5) RDN sequence of different Case.
             * 6) Matching binary return values.
             * 7) Binary values that don't match.
             */
            String names1[] = new String [] {
                "ou=Sales+cn=Bob", "ou=Sales+cn=Bob", "ou=Sales+cn=Bob",
                "ou=Sales+cn=Scott+c=US", "cn=config"};

            String names2[] = new String [] {
                "ou=Sales+cn=Bob", "cn=Bob+ou=Sales", "ou=Sales+cn=Scott",
                "ou=Sales+cn=Scott", "Cn=COnFIG"};

            int expectedResults[] = {0, 0, -1, -1, 0};

            for (Locale locale : Locale.getAvailableLocales()) {
                // reset the default locale
                Locale.setDefault(locale);

                for (int i = 0; i < names1.length; i++) {
                    checkResults(new LdapName(names1[i]),
                        new LdapName(names2[i]), expectedResults[i]);
                }

                byte[] value = "abcxyz".getBytes();
                Rdn rdn1 = new Rdn("binary", value);
                ArrayList<Rdn> rdns1 = new ArrayList<>();
                rdns1.add(rdn1);
                LdapName l1 = new LdapName(rdns1);

                Rdn rdn2 = new Rdn("binary", value);
                ArrayList<Rdn> rdns2 = new ArrayList<>();
                rdns2.add(rdn2);
                LdapName l2 = new LdapName(rdns2);
                checkResults(l1, l2, 0);

                l2 = new LdapName("binary=#61626378797A");
                checkResults(l1, l2, 0);

                l2 = new LdapName("binary=#61626378797B");
                checkResults(l1, l2, -1);

                System.out.println("Tests passed");
            }
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }


    static void checkResults(LdapName name1, LdapName name2, int expectedResult)
                throws Exception {

        System.out.println("Checking name1: " + name1 +
                " and name2: " + name2);

        boolean isEquals = (expectedResult == 0) ? true : false;

        int result = name1.compareTo(name2);
        if ((isEquals && (result != expectedResult)) ||
                isPositive(result) != isPositive(expectedResult)) {
            throw new Exception(
                "Comparison test failed for name1:" +
                name1 + " name2:" + name2 +
                ", expected (1 => positive, -1 => negetive): " +
                expectedResult + " but got: " + result);
        }

        if (name1.equals(name2) != isEquals) {
            throw new Exception("Equality test failed for name1: " +
                        name1 + " name2:" + name2 + ", expected: " +
                        isEquals);
        }

        if (isEquals && (name1.hashCode() != name2.hashCode())) {
           System.out.println("name1.hashCode(): " + name1.hashCode() +
                                " name2.hashCode(): " + name2.hashCode());
            throw new Exception("hashCode test failed for name1:" +
                        name1 + " name2:" + name2);
        }
    }

    static boolean isPositive(int n) {
        return (n >= 0) ? true : false;
    }
}
