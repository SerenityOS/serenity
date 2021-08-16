/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4256589
 * @summary Test if getPackage() and getPackages()
 *          return consistent values.
 */

public class GetPackage {
    public static void main(String arg[]) throws Exception {
        TestClassLoader parent = new TestClassLoader();
        TestClassLoader child = new TestClassLoader(parent);
        // child define a package first
        child.defineEmptyPackage("foo");
        // parent then define another package with the same name
        parent.defineEmptyPackage("foo");
        if (!child.testPackageView("foo"))
            throw new Exception("Inconsistent packages view");
    }
}

class TestClassLoader extends ClassLoader {
    public TestClassLoader() {
        super();
    }

    public TestClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Package defineEmptyPackage(String name) {
        return definePackage(name, null, null, null, null, null, null, null);
    }

    /* test to see if getPackage() and getPackages()
     * are consistent.
     */
    public boolean testPackageView(String name) {
        Package[] pkgs = getPackages();
        Package pkg = getPackage(name);
        for(int i = 0; i < pkgs.length; i++)
            if (pkgs[i].getName().equals(name) && pkgs[i] == pkg)
                return true;
        return false;
    }
}
