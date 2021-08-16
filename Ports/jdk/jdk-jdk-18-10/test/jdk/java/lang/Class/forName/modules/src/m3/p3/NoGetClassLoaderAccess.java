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

package p3;

import java.security.AccessControlException;
import java.security.Permission;

/**
 * Verify RuntimePermission("getClassLoader") is needed to load
 * a class in another module
 */
public class NoGetClassLoaderAccess {
    private static final Module m3 = NoGetClassLoaderAccess.class.getModule();
    private static final Permission GET_CLASSLOADER_PERMISSION = new RuntimePermission("getClassLoader");

    public static void main(String[] args) throws Exception {
        ModuleLayer boot = ModuleLayer.boot();

        System.setSecurityManager(new SecurityManager());
        Module m1 = boot.findModule("m1").get();
        Module m2 = boot.findModule("m2").get();
        findClass(m1, "p1.A");
        findClass(m1, "p1.internal.B");
        findClass(m2, "p2.C");
        findClass(m3, "p3.internal.Foo");
    }

    static Class<?> findClass(Module module, String cn) {
        try {
            Class<?> c = Class.forName(module, cn);
            if (c == null) {
                throw new RuntimeException(cn + " not found in " + module);
            }
            if (c.getModule() != module) {
                throw new RuntimeException(c.getModule() + " != " + module);
            }
            return c;
        } catch (AccessControlException e) {
            if (module != m3) {
                if (e.getPermission().equals(GET_CLASSLOADER_PERMISSION))
                    return null;
            }
            throw e;
        }
    }
}
