/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8191053
 * @summary Test that the allow/disallow options of the java.security.manager
 *          system property work correctly
 * @run main/othervm AllowSecurityManager
 * @run main/othervm -Djava.security.manager=disallow AllowSecurityManager
 * @run main/othervm -Djava.security.manager=allow AllowSecurityManager
 */

public class AllowSecurityManager {

    public static void main(String args[]) throws Exception {
        String prop = System.getProperty("java.security.manager");
        boolean disallow = "disallow".equals(prop);
        try {
            System.setSecurityManager(new SecurityManager());
            if (disallow) {
                throw new Exception("System.setSecurityManager did not " +
                                    "throw UnsupportedOperationException");
            }
        } catch (UnsupportedOperationException uoe) {
            if (!disallow) {
                throw new Exception("UnsupportedOperationException " +
                                    "unexpectedly thrown by " +
                                    "System.setSecurityManager");
            }
        }
    }
}
