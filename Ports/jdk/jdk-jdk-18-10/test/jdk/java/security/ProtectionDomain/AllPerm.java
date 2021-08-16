/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6256734
 * @summary ProtectionDomain could optimize implies by first checking for
 *          AllPermission in internal collection
 * @run main/othervm -Djava.security.manager=allow AllPerm
 */

import java.io.*;
import java.net.*;
import java.security.*;
import java.lang.reflect.*;

public class AllPerm {

    private static final Class[] ARGS = new Class[] { };
    private static ProtectionDomain allPermClassDomain;

    public static void main(String[]args) throws Exception {

        // create custom class loader that assigns AllPermission to
        // classes it loads

        File file = new File(System.getProperty("test.src"), "AllPerm.jar");
        URL[] urls = new URL[] { file.toURL() };
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        AllPermLoader loader = new AllPermLoader(urls, parent);

        // load a class from AllPerm.jar using custom loader

        Object o = loader.loadClass("AllPermClass").newInstance();
        Method doCheck = o.getClass().getMethod("doCheck", ARGS);
        allPermClassDomain = o.getClass().getProtectionDomain();

        // set a custom Policy and set the SecurityManager

        Policy.setPolicy(new AllPermPolicy());
        System.setSecurityManager(new SecurityManager());

        // invoke method on loaded class, which will perform a
        // security-sensitive operation.  custom policy will check
        // to see if it is called (it should not be called)

        doCheck.invoke(o, ARGS);
    }

    /**
     * this class loader assigns AllPermission to classes that it loads
     */
    private static class AllPermLoader extends URLClassLoader {

        public AllPermLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        protected PermissionCollection getPermissions(CodeSource codesource) {
            Permissions perms = new Permissions();
            perms.add(new AllPermission());
            return perms;
        }
    }

    /**
     * this policy should not be called if domain is allPermClassDomain
     */
    private static class AllPermPolicy extends Policy {
        public boolean implies(ProtectionDomain domain, Permission permission) {
            if (domain == allPermClassDomain) {
                throw new SecurityException
                        ("Unexpected call into AllPermPolicy");
            }
            return true;
        }
    }
}

/**
 * here is the source code for AllPermClass inside AllPerm.jar
 */
/*
public class AllPermClass {
    public void doCheck() {
        System.getProperty("user.name");
    }
}
*/
