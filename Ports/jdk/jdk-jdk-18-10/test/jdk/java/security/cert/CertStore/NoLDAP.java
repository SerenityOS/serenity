/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8004502
 * @summary Sanity check that NoSuchAlgorithmException is thrown when requesting
 *   a CertStore of type "LDAP" and LDAP is not available.
 */

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertStore;
import java.security.cert.LDAPCertStoreParameters;


public class NoLDAP {
    public static void main(String[] args) throws Exception {
        try {
            Class.forName("javax.naming.ldap.LdapName");
            System.out.println("LDAP is present, test skipped");
            return;
        } catch (ClassNotFoundException ignore) { }

        try {
            CertStore.getInstance("LDAP", new LDAPCertStoreParameters());
            throw new RuntimeException("NoSuchAlgorithmException expected");
        } catch (NoSuchAlgorithmException x) {
            System.out.println("NoSuchAlgorithmException thrown as expected");
        }
    }
}
