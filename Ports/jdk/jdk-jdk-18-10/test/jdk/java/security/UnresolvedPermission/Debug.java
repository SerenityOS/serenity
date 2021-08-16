/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4350951
 * @summary     UnresolvedPermission assumes permission constructor
 *              with 2 string parameters
 *
 * @compile Debug.java DebugPermissionBad.java DebugPermission0.java DebugPermission1.java DebugPermission2.java
 * @run main/othervm/policy=Debug.policy -Djava.security.debug=policy,access Debug
 */

public class Debug {

    public static void main(String[] args) {

        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            throw new SecurityException("Test Failed: no SecurityManager");
        }

        try {
            sm.checkPermission(new DebugPermissionBad("hello", 1));
            throw new SecurityException("Test 1 Failed: no SecurityException");
        } catch (SecurityException se) {
            // good
        }

        try {
            sm.checkPermission(new DebugPermission0());
        } catch (SecurityException se) {
            throw new SecurityException("Test 2 Failed");
        }

        try {
            sm.checkPermission(new DebugPermission1("1"));
        } catch (SecurityException se) {
            throw new SecurityException("Test 3 Failed");
        }

        try {
            sm.checkPermission(new DebugPermission2("1", "2"));
        } catch (SecurityException se) {
            throw new SecurityException("Test 4 Failed");
        }

        System.out.println("Test Succeeded");
    }
}
