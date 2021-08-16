/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.security.cert.CertStore;
import java.security.cert.URICertStoreParameters;
import java.net.URI;

/*
 * @test
 * @bug 8038084
 * @summary Basic testing for URICertStoreParameters
 * @run main TestBasic
 */
public class TestBasic {

    public static void main(String[] args) throws Exception {
        String str1 = "ldap://myownhost:5000/";
        String str2 = "ldap://myownhost:5000/cn=foo";
        test(str1, str2);
        System.out.println("Test passed");
    }

    private static void test(String str1, String str2) throws Exception {
        System.out.println("Testing clone");
        URICertStoreParameters p1 = new URICertStoreParameters(new URI(str1));
        URICertStoreParameters p1Too = p1.clone();
        if (!(p1.getURI().equals(p1Too.getURI())) ||
            !(p1.equals(p1Too))) {
            throw new Exception("Error: problem with clone");
        }
        System.out.println("Testing equal and hashCode");
        p1Too = new URICertStoreParameters(new URI(str1));
        URICertStoreParameters p2 = new URICertStoreParameters(new URI(str2));

        if (p1.equals(null)) {
            throw new Exception("Error: p1 should NOT equal null");
        }
        if ((!p1.equals(p1)) || (p1.hashCode() != p1.hashCode())) {
            throw new Exception("Error: p1 should equal p1");
        }
        if ((!p1.equals(p1Too)) || (p1.hashCode() != p1Too.hashCode())) {
            throw new Exception("Error: p1 should equal p1Too");
        }
        if ((!p1Too.equals(p1)) || (p1Too.hashCode() != p1.hashCode())) {
            throw new Exception("Error: p1Too should equal p1");
        }
        if (p1.equals(p2) || p1Too.equals(p2)) {
            throw new Exception("Error: p1/p1Too should NOT equal p2");
        }
    }
}
