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

/*
 * @test
 * @bug 8034820
 * @summary Check that adding Principal subclasses to Subject works
 */

import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import javax.security.auth.Subject;

public class AddPrincipalSubclass {

    public static void main(String[] args) throws Exception {

        // create Subject with no principals and then add TestPrincipal
        Subject subject = new Subject();
        Set<Principal> principals = subject.getPrincipals(Principal.class);
        Principal principal = new TestPrincipal();
        if (!principals.add(principal)) {
            throw new Exception("add returned false instead of true");
        }
        if (!principals.contains(principal)) {
            throw new Exception("set does not contain principal");
        }

        // pre-populate Subject with TestPrincipal
        subject = new Subject(false,
                              Collections.singleton(principal),
                              Collections.emptySet(), Collections.emptySet());
        principals = subject.getPrincipals(Principal.class);
        if (!principals.contains(principal)) {
            throw new Exception("set does not contain principal");
        }
    }

    private static class TestPrincipal implements Principal {
        @Override
        public String getName() {
            return "TestPrincipal";
        }
    }
}
