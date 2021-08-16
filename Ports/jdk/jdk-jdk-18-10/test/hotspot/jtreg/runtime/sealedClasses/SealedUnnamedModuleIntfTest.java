/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8225056
 * @compile Pkg/SealedInterface.jcod Pkg/NotPermitted.jcod
 * @compile Pkg/Permitted.java otherPkg/WrongPackage.java otherPkg/WrongPackageNotPublic.java
 * @run main SealedUnnamedModuleIntfTest
 */

public class SealedUnnamedModuleIntfTest {

    public static void main(String args[]) throws Throwable {

        // Classes Permitted, NotPermitted, WrongPackage and WrongPackageNotPublic all try to implement
        // sealed interface SealedInterface.
        // Interface SealedInterface permits classes Permitted, WrongPackage, and WrongPackageNotPublic;

        // Test non-public permitted subclass and superclass in unnamed module and
        // same package.  This should succeed.
        Class permitted = Class.forName("Pkg.Permitted");

        // Test public permitted subclass and superclass in same unnamed module but in
        // different packages.  This should not throw an exception.
        Class wrongPkg = Class.forName("otherPkg.WrongPackage");

        // Test unpermitted subclass and superclass in unnamed module and same package.
        // This should throw an exception.
        try {
            Class notPermitted = Class.forName("Pkg.NotPermitted");
            throw new RuntimeException("Expected IncompatibleClassChangeError exception not thrown");
        } catch (IncompatibleClassChangeError e) {
            if (!e.getMessage().contains("cannot implement sealed interface")) {
                throw new RuntimeException("Wrong IncompatibleClassChangeError exception thrown: " + e.getMessage());
            }
        }

        // Test non-public permitted subclass and superclass in same unnamed module but
        // in different packages.  This should throw an exception.
        try {
            Class notPermitted = Class.forName("otherPkg.WrongPackageNotPublic");
            throw new RuntimeException("Expected IncompatibleClassChangeError exception not thrown");
        } catch (IncompatibleClassChangeError e) {
            if (!e.getMessage().contains("cannot implement sealed interface")) {
                throw new RuntimeException("Wrong IncompatibleClassChangeError exception thrown: " + e.getMessage());
            }
        }
    }
}
