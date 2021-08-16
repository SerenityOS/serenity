/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @author Vincent Ryan
 * @bug 6393770
 * @summary Check that an LdapPrincipal can be initialized using various forms
 *          of string distinguished names.
 */

import java.util.*;
import java.security.Principal;
import javax.security.auth.Subject;
import com.sun.security.auth.LdapPrincipal;

/*
 * Create principals using string distinguished names containing non-standard
 * attribute names.  A non-standard attribute name is one that is not listed in
 * Section 2.3 of RFC 2253. Typically, such distinguished names are valid in
 * LDAP but are not valid in X.500 as they cannot be encoded using ASN.1 BER
 * (because the object identifier corresponding to the attribute name is
 * unknown).
 */
public class CreateLdapPrincipals {

    public static void main(String[] args) throws Exception {

        Set<Principal> principals = new Subject().getPrincipals();

        principals.add(new LdapPrincipal("x=y"));
        principals.add(new LdapPrincipal("x=#04024869"));
        principals.add(new LdapPrincipal("1.2.3=x"));
        principals.add(new LdapPrincipal("A=B"));
        principals.add(new LdapPrincipal("a=b+c=d"));
        principals.add(new LdapPrincipal("a=b,c=d,e=f"));
        principals.add(new LdapPrincipal("f=g, h=i, j=k"));

        System.out.println("Successfully created " + principals.size() +
            " LDAP principals:");
        System.out.println(principals);
    }
}
