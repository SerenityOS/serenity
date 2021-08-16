/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @bug 4619757
 * @summary User Policy Setting is not recognized on Netscape 6
 *          when invoked as root.
 * @run main/manual Root
 */

/*
 * Place Root.policy in the root home directory (/),
 * as /.java.policy and run as test as root user.
 */

import java.security.*;

public class Root {
    public static void main(String[] args) {
        Policy p = Policy.getPolicy();
        if (p.implies(Root.class.getProtectionDomain(), new AllPermission())) {
            System.out.println("Test succeeded");
        } else {
            throw new SecurityException("Test failed");
        }
    }
}
