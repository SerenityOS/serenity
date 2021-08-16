/*
 * Copyright (c) 2002, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4513737
 * @run main/othervm Equals
 * @summary UnresolvedPermission.equals() throws NullPointerException
 */

import java.security.*;
import java.util.*;

public class Equals {
    public static void main(String[] args) {
        if (System.getProperty("test.src") == null) {
            System.setProperty("test.src", ".");
        }
        System.setProperty("java.security.policy",
                "file:${test.src}/Equals.policy");
        PermissionCollection pc = Policy.getPolicy().getPermissions
                        (Equals.class.getProtectionDomain());
        ArrayList l = new ArrayList();
        for (Enumeration e = pc.elements(); e.hasMoreElements();) {
            Object p = e.nextElement();
            if (p instanceof UnresolvedPermission) {
                l.add(p);
            }
        }
        System.out.println(l.get(0) + "\n" + l.get(1));
        System.out.println(l.get(0).equals(l.get(1)));
    }
}
