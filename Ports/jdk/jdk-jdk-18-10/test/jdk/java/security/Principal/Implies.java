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

/*
 * @test
 * @bug 7019834
 * @summary test default implementation of Principal.implies
 */

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;

public class Implies {
    public static void main(String[] args) throws Exception {
        X500Principal duke = new X500Principal("CN=Duke");
        // should not throw NullPointerException
        testImplies(duke, (Subject)null, false);

        Set<Principal> principals = new HashSet<>();
        principals.add(duke);
        testImplies(duke, principals, true);

        X500Principal tux = new X500Principal("CN=Tux");
        principals.add(tux);
        testImplies(duke, principals, true);

        principals.add(new KerberosPrincipal("duke@java.com"));
        testImplies(duke, principals, true);

        principals.clear();
        principals.add(tux);
        testImplies(duke, principals, false);

        System.out.println("test passed");
    }

    private static void testImplies(Principal principal,
                                    Set<? extends Principal> principals,
                                    boolean result)
        throws SecurityException
    {
        Subject subject = new Subject(true, principals, Collections.emptySet(),
                                      Collections.emptySet());
        testImplies(principal, subject, result);
    }

    private static void testImplies(Principal principal,
                                    Subject subject, boolean result)
        throws SecurityException
    {
        if (principal.implies(subject) != result) {
            throw new SecurityException("test failed");
        }
    }
}
