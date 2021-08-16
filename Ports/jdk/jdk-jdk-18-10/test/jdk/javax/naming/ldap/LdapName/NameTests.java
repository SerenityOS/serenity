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
import java.util.*;
import javax.naming.*;
import java.io.*;

/**
 * LdapName tests- These tests are for testing the methods of
 * javax.naming.Name interface as it applied to LdapName.
 */
public class NameTests {

    public static void main(String args[]) throws Exception {

        String[] rdnStr = new String[] {"one=voilet"};

        ArrayList<Rdn> rdnList = new ArrayList<>();

        for (int i = 0; i < rdnStr.length; i++) {
            rdnList.add(i, new Rdn(rdnStr[i]));
        }
        LdapName dn = new LdapName(rdnList);

        Collection<Rdn> rdns = dn.getRdns();
        System.out.println("size is :" + dn.size());
        System.out.println("isEmpty :" + dn.isEmpty());
        System.out.println("************Printing as Rdns*********");
        Iterator<Rdn> iter = rdns.iterator();
        while (iter.hasNext()) {
            System.out.println(iter.next());
        }

        System.out.println();
        System.out.println("************Printing the Enumeration*********");
        Enumeration<String> dnEnum = dn.getAll();
        while (dnEnum.hasMoreElements()) {
            System.out.println(dnEnum.nextElement());
        }

        // addAll tests
        System.out.println();
        LdapName nameSuffix = new LdapName("two=Indigo");
        System.out.println("addAll():" + dn.addAll(nameSuffix));

        ArrayList<Rdn> list = new ArrayList<>();
        list.add(new Rdn("five=Yellow"));
        System.out.println("Rdn- addAll():" + dn.addAll(list));

        nameSuffix = new LdapName("three=Blue");
        System.out.println();
        System.out.println("addAll at pos = 2");
        System.out.println("addAll():" + dn.addAll(2, nameSuffix));

        list = new ArrayList<>();
        list.add(new Rdn("four=Green"));
        System.out.println();
        System.out.println("addAll at pos = 3");
        System.out.println("Rdn- addAll():" + dn.addAll(3, list));

        // add() tests
        Rdn rdn;
        System.out.println();
        System.out.println("add():" + dn.add("eight=white"));
        rdn = new Rdn("nine=Black");
        System.out.println();
        System.out.println("Rdn- add():" + dn.add(rdn));

        /*
        Rdn nullRdn = null;
        System.out.println("Rdn- add() with null RDN:" +
                        dn.add(nullRdn));
        */

        System.out.println();
        System.out.println("add() at pos 5");
        System.out.println("add():" + dn.add(5, "six=Orange"));
        rdn = new Rdn("six=Orange");
        System.out.println();
        System.out.println("add() at pos 6");
        System.out.println("Rdn- add():" + dn.add(6, "seven=Red"));

        // remove tests
        System.out.println();
        System.out.println("Removing entries at positions: 7, 8");
        System.out.println("Removed:" + dn.remove(8));
        System.out.println("Removed:" + dn.remove(7));

        // get tests
        System.out.println();
        System.out.println("toString():" + dn);
        int size  = dn.size();
        System.out.println("get(0):" + dn.get(0));
        System.out.println("get(size() - 1):" + dn.get(size - 1));
        System.out.println("getRdn(0):" + dn.getRdn(0));
        System.out.println("getRdn(size() - 1):" + dn.getRdn(size - 1));

        System.out.println();
        System.out.println("********Prefixes**********");
        System.out.println("getPrefix(0):" + dn.getPrefix(0));
        System.out.println("getPrefix(size / 2):" + dn.getPrefix(size / 2));
        System.out.println("getPrefix(size):" + dn.getPrefix(size));

        System.out.println();
        System.out.println("********Suffixes**********");
        System.out.println("getSuffix(0):" + dn.getSuffix(0));
        System.out.println("getSuffix(size/2):" + dn.getSuffix(size / 2));
        System.out.println("getSuffix(size):" + dn.getSuffix(size));

        System.out.println();
        System.out.println("startsWith(" + rdnStr[0] + "):" +
                                dn.startsWith(new LdapName(rdnStr[0])));

        String lastEntry = "seven=red";
        System.out.println("startsWith(" + lastEntry + "):" +
                                dn.startsWith(new LdapName(lastEntry)));

        System.out.println("compositeName- startsWith(" +
                        rdnStr[0] + "): " + dn.startsWith(
                                        new CompositeName(rdnStr[0])));

        List<Rdn> prefixList = (dn.getRdns()).subList(0, size /2);
        System.out.println("Rdn - startsWith(" + prefixList + "):" +
                                dn.startsWith(prefixList));

        System.out.println("Rdn - startsWith() - empty RDN list:" +
                                dn.startsWith(new ArrayList<>()));

        System.out.println();
        System.out.println("endsWith(" + rdnStr[0] + "):" +
                                dn.endsWith(new LdapName(rdnStr[0])));

        System.out.println("endsWith(" + lastEntry + "):" +
                                dn.endsWith(new LdapName(lastEntry)));

        System.out.println("compositeName- endsWith(" + rdnStr[0] + "):    " +
                dn.endsWith(new CompositeName(rdnStr[0])));

        System.out.println("Rdn - endsWith(" + prefixList + "):" +
                                dn.endsWith(prefixList));

        System.out.println("Rdn - endsWith() empty RDN list:" +
                                dn.endsWith(new ArrayList<>()));

        // test clone
        System.out.println();
        System.out.println("cloned name:" + dn.clone());

        // test serialization
        ObjectOutputStream out = new ObjectOutputStream(
                                    new FileOutputStream("dn.ser"));
        out.writeObject(dn);
        out.close();

        ObjectInputStream in = new ObjectInputStream(
                                    new FileInputStream("dn.ser"));

        System.out.println();
        System.out.println("Deserialized name:" + in.readObject());
        in.close();
    }
}
