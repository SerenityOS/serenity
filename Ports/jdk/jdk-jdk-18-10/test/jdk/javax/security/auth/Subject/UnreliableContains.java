/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8243592
 * @summary Subject$SecureSet::addAll should not call contains(null)
 */

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

public class UnreliableContains {

    public static void main(String[] args) {
        MySet<Principal> set = new MySet<>();
        set.add(null);
        Subject s = null;
        try {
            s = new Subject(false, set, Collections.emptySet(),
                    Collections.emptySet());
        } catch (NullPointerException e) {
            // The correct exit
            return;
        }
        // Suppose NPE was not caught. At least null was not added?
        for (Principal p : s.getPrincipals()) {
            Objects.requireNonNull(p);
        }
        // Still must fail. We don't want this Subject created
        throw new RuntimeException("Fail");
    }

    // This is a Map that implements contains(null) differently
    static class MySet<E> extends HashSet<E> {
        @Override
        public boolean contains(Object o) {
            if (o == null) {
                return false;
            } else {
                return super.contains(o);
            }
        }
    }
}
