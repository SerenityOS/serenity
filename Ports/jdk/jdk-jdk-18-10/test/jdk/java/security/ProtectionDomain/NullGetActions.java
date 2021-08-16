/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8043252
 * @summary Debug of access control is obfuscated - NullPointerException in
 *          ProtectionDomain
 * @run main/othervm/java.security.policy=NullGetActions.policy NullGetActions
 */

import java.net.*;
import java.security.*;

public class NullGetActions {

    public static void main(String[] args) throws Exception {
        Permissions permset = new Permissions();
        permset.add(new EvilPermission("java.let.me.do.stuff"));

        Policy.getPolicy();
        ProtectionDomain protDom = new ProtectionDomain(
                new CodeSource(new URL("http://bar"),
                        (java.security.cert.Certificate[])null), permset,
                        null, null);

        System.out.println("Protection Domain:\n" + protDom);
    }

    public static class EvilPermission extends Permission {
        public EvilPermission(String name) {
            super(name);
        }

        @Override
        public String getActions() {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj == this);
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean implies(Permission permission) {
            return false;
        }
    }
}
