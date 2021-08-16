/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6789935 8012615
 * @modules java.security.jgss/sun.security.krb5
 * @run main/othervm ParseCAPaths
 * @summary cross-realm capath search error
 */

import java.util.Arrays;
import sun.security.krb5.Realm;

public class ParseCAPaths {
    static Exception failed = null;
    public static void main(String[] args) throws Exception {
        System.setProperty("java.security.krb5.conf",
                System.getProperty("test.src", ".") +"/krb5-capaths.conf");

        // MIT
        check("ANL.GOV", "TEST.ANL.GOV", "ANL.GOV");
        check("ANL.GOV", "ES.NET", "ANL.GOV");
        check("ANL.GOV", "PNL.GOV", "ANL.GOV", "ES.NET");
        check("ANL.GOV", "NERSC.GOV", "ANL.GOV", "ES.NET");
        check("NERSC.GOV", "TEST.ANL.GOV", "NERSC.GOV", "ES.NET", "ANL.GOV");

        // RedHat
        // 3.6.2.1. Configuring a Shared Hierarchy of Names
        check("AA.EXAMPLE.COM", "BB.EXAMPLE.COM",
                "AA.EXAMPLE.COM", "EXAMPLE.COM");
        check("SITE1.SALES.EXAMPLE.COM", "EVERYWHERE.EXAMPLE.COM",
                "SITE1.SALES.EXAMPLE.COM", "SALES.EXAMPLE.COM",
                "EXAMPLE.COM");
        check("DEVEL.EXAMPLE.COM", "PROD.EXAMPLE.ORG",
                "DEVEL.EXAMPLE.COM", "EXAMPLE.COM", "COM",
                "ORG", "EXAMPLE.ORG");
        // 3.6.2.2. Configuring Paths in krb5.conf
        check("A.EXAMPLE.COM", "B.EXAMPLE.COM", "A.EXAMPLE.COM");
        check("A.EXAMPLE.COM", "C.EXAMPLE.COM",
                "A.EXAMPLE.COM", "B.EXAMPLE.COM");
        check("A.EXAMPLE.COM", "D.EXAMPLE.COM",
                "A.EXAMPLE.COM", "B.EXAMPLE.COM", "C.EXAMPLE.COM");

        // The original JDK example
        check("TIVOLI.COM", "IBM.COM", "TIVOLI.COM", "LDAPCENTRAL.NET",
            "IBM_LDAPCENTRAL.COM", "MOONLITE.ORG");

        // Hierachical
        check("N1.N.COM", "N2.N.COM", "N1.N.COM", "N.COM");
        check("N1.N.COM", "N2.N3.COM", "N1.N.COM", "N.COM",
                "COM", "N3.COM");
        check("N1.COM", "N2.COM", "N1.COM", "COM");
        check("N1", "N2", "N1");
        check("N1.COM", "N2.ORG", "N1.COM", "COM", "ORG");
        check("N1.N.COM", "N.COM", "N1.N.COM");
        check("X.N1.N.COM", "N.COM", "X.N1.N.COM", "N1.N.COM");
        check("N.COM", "N1.N.COM", "N.COM");
        check("N.COM", "X.N1.N.COM", "N.COM", "N1.N.COM");
        check("A.B.C", "D.E.F", "A.B.C", "B.C", "C", "F", "E.F");

        // Full path
        check("A1.COM", "A2.COM", "A1.COM");
        check("A1.COM", "A3.COM", "A1.COM", "A2.COM");
        check("A1.COM", "A4.COM", "A1.COM", "A2.COM", "A3.COM");

        // Shortest path
        check("B1.COM", "B2.COM", "B1.COM");
        check("B1.COM", "B3.COM", "B1.COM", "B2.COM");
        check("B1.COM", "B4.COM", "B1.COM", "B2.COM", "B3.COM");

        // Missing is "."
        check("C1.COM", "C2.COM", "C1.COM", "COM");
        check("C1.COM", "C3.COM", "C1.COM", "C2.COM");

        // cRealm = .
        check("D1.COM", "D2.COM", "D1.COM");

        // Bad cases
        check("E1.COM", "E2.COM", "E1.COM");
        check("E1.COM", "E3.COM", "E1.COM", "E4.COM");
        check("G1.COM", "G3.COM", "G1.COM", "G2.COM");
        check("I1.COM", "I4.COM", "I1.COM", "I5.COM");

        // 7019384
        check("A9.PRAGUE.XXX.CZ", "SERVIS.XXX.CZ",
                "A9.PRAGUE.XXX.CZ", "PRAGUE.XXX.CZ", "ROOT.XXX.CZ");

        if (failed != null) {
            throw failed;
        }
    }

    static void check(String from, String to, String... paths) {
        try {
            check2(from, to, paths);
        } catch (Exception e) {
            System.out.println("         " + e.getMessage());
            failed = e;
        }
    }

    static void check2(String from, String to, String... paths)
            throws Exception {
        System.out.println(from + " -> " + to);
        System.out.println("    expected: " + Arrays.toString(paths));
        String[] result = Realm.getRealmsList(from, to);
        if (result == null || result.length == 0) {
            throw new Exception("There is always a valid path.");
        } else if(result.length != paths.length) {
            throw new Exception("Length of path not correct");
        } else {
            for (int i=0; i<result.length; i++) {
                if (!result[i].equals(paths[i])) {
                    System.out.println("    result:   " + Arrays.toString(result));
                    throw new Exception("Path not same");
                }
            }
        }
    }
}
