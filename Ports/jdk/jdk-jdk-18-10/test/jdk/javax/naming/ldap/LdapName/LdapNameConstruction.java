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
 * @bug 4635618
 * @summary Support for manipulating LDAP Names
 */

import javax.naming.ldap.*;
import java.util.ArrayList;
import java.util.List;
import javax.naming.InvalidNameException;
import javax.naming.directory.*;

/**
 * These tests are for checking the LdapName and Rdn
 * constructors.
 */
public class LdapNameConstruction {

    public static void main(String args[])
                throws Exception {
        /**
         * Four ways of constructing the same Rdn
         */
        Rdn rdn1 = new Rdn("ou= Juicy\\, Fruit");
        System.out.println("rdn1:" + rdn1.toString());

        Rdn rdn2 = new Rdn(rdn1);
        System.out.println("rdn2:" + rdn2.toString());

        Attributes attrs = new BasicAttributes();
        attrs.put("ou", "Juicy, Fruit");
        attrs.put("cn", "Mango");
        Rdn rdn3 = new Rdn(attrs);
        System.out.println("rdn3:" + rdn3.toString());

        Rdn rdn4 = new Rdn("ou", "Juicy, Fruit");
        System.out.println("rdn4:" + rdn4.toString());

        // Rdn with unicode value
        Rdn rdn5 = new Rdn("SN=Lu\\C4\\8Di\\C4\\87");
        System.out.println("rdn5:" + rdn5.toString());

        /**
         * LdapName creation tests
         */
        List<Rdn> rdns = new ArrayList<>();
        rdns.add(new Rdn("o=Food"));
        rdns.add(new Rdn("ou=Fruits"));
        rdns.add(rdn3);
        LdapName name1 = new LdapName(rdns);
        System.out.println("ldapname1:" + name1.toString());

        LdapName name2 = new LdapName(
                "ou=Juicy\\, Fruit + cn = Mango, ou=Fruits, o=Food");
        System.out.println("ldapName2:" + name2.toString());

        if (!name2.equals(name1)) {
            throw new Exception("ldapname1 does not equals ldapname2");
        }
        System.out.println("ldapname1 and ldapname2 are equal");

        LdapName name = new LdapName(new ArrayList<>());
        System.out.println("Empty ldapname:" + name);
    }
}
