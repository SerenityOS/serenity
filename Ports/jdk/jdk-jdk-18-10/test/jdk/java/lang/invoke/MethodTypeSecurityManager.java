/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/* @test
 * @bug 8229785
 * @summary Test MethodType.fromMethodDescriptorString with security manager
 * @run main/othervm -Djava.security.manager=allow test.java.lang.invoke.MethodTypeSecurityManager
 * @run main/othervm/policy=getclassloader.policy test.java.lang.invoke.MethodTypeSecurityManager access
 */

package test.java.lang.invoke;

import java.lang.invoke.MethodType;
import java.security.AccessControlException;
import java.security.Permission;

public class MethodTypeSecurityManager {
    private static boolean hasClassLoaderAccess;
    public static void main(String... args) throws Throwable {
        ClassLoader platformLoader = ClassLoader.getPlatformClassLoader();
        ClassLoader appLoader = ClassLoader.getSystemClassLoader();
        hasClassLoaderAccess = args.length == 1 && "access".equals(args[0]);

        assert hasClassLoaderAccess || System.getSecurityManager() == null;
        if (!hasClassLoaderAccess) {
            System.setSecurityManager(new SecurityManager());
        }

        // require getClassLoader permission
        throwACC("()Ljdk/internal/misc/VM;", null);
        // package access check when app class loader loads the class
        throwACC("()Ljdk/internal/misc/VM;", appLoader);

        // if using the platform class loader, no package access check
        MethodType.fromMethodDescriptorString("()Ljdk/internal/misc/VM;", platformLoader);
    }

    private static void throwACC(String desc, ClassLoader loader) {
        try {
            MethodType.fromMethodDescriptorString(desc, loader);
            throw new RuntimeException("should never leak JDK internal class");
        } catch (AccessControlException e) {
            System.out.println(e.getMessage());
            Permission perm = e.getPermission();
            if (!(perm instanceof RuntimePermission)) throw e;
            // ACC thrown either no "getClassLoader" permission or no package access
            switch (perm.getName()) {
                case "getClassLoader":
                    if (!hasClassLoaderAccess) break;
                case "accessClassInPackage.jdk.internal.misc":
                    break;
                default:
                    throw e;
            }
        }
    }
}
