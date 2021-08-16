/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8022444 8242151
 * @summary Test ObjectIdentifier.equals(Object obj)
 * @modules java.base/sun.security.util
 */

import sun.security.util.ObjectIdentifier;

public class OidEquals {
    public static void main(String[] args) throws Exception {
        ObjectIdentifier oid1 = ObjectIdentifier.of("1.3.6.1.4.1.42.2.17");
        ObjectIdentifier oid2 = ObjectIdentifier.of("1.2.3.4");

        assertEquals(oid1, oid1);
        assertNotEquals(oid1, oid2);
        assertNotEquals(oid1, "1.3.6.1.4.1.42.2.17");

        System.out.println("Tests passed.");
    }

    static void assertEquals(ObjectIdentifier oid, Object obj)
            throws Exception {
        if (!oid.equals(obj)) {
            throw new Exception("The ObjectIdentifier " + oid.toString() +
                    " should be equal to the Object " + obj.toString());
        }
    }

    static void assertNotEquals(ObjectIdentifier oid, Object obj)
            throws Exception {
        if (oid.equals(obj)) {
            throw new Exception("The ObjectIdentifier " + oid.toString() +
                    " should not be equal to the Object " + obj.toString());
        }
    }
}
