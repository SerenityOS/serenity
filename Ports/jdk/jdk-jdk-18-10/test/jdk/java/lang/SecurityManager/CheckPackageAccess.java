/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 *  @test
 *  @bug 6741606 7146431 8000450 8019830 8022945 8027144 8041633 8078427 8055206
 *  @summary Check that various restricted packages that are supposed to be
 *           restricted by default or are listed in the package.access
 *           property in the java.security file are blocked
 *  @run main/othervm CheckPackageAccess
 */

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CheckPackageAccess {

    private static final SecurityManager sm = new SecurityManager();
    private static final ModuleFinder mf = ModuleFinder.ofSystem();

    /*
     * The expected list of restricted packages of the package.access property.
     *
     * This array should be updated whenever new packages are added to the
     * package.access property in the java.security file
     * NOTE: it should be in the same order as the java.security file
     */
    private static final String[] EXPECTED = {
        "sun.misc.",
        "sun.reflect.",
    };

    /**
     * Tests access to various packages of a module.
     */
    private static class Test {
        String moduleName;     // name of module
        ModuleReference moduleRef;     // module reference
        String exports;    // exported pkg
        Optional<String> opens;      // opened pkg
        String conceals;   // concealed pkg
        Optional<String> qualExports; // qualified export pkg
        Optional<String> qualOpens;   // qualified open pkg
        // qual open and non-qualified export pkg
        Optional<String> qualOpensAndExports;
        Test(String module, String exports, String opens, String conceals,
             String qualExports, String qualOpens, String qualOpensAndExports) {
            this.moduleName = module;
            this.moduleRef = mf.find(moduleName).get();
            this.exports = exports;
            this.opens = Optional.ofNullable(opens);
            this.conceals = conceals;
            this.qualExports = Optional.ofNullable(qualExports);
            this.qualOpens = Optional.ofNullable(qualOpens);
            this.qualOpensAndExports = Optional.ofNullable(qualOpensAndExports);
        }

        void test() {
            final boolean isModulePresent =
                        ModuleLayer.boot().findModule(moduleName).isPresent();
            System.out.format("Testing module: %1$s. Module is%2$s present.\n",
                        moduleName, isModulePresent ? "" : " NOT");

            if (isModulePresent) {

                // access to exported pkg should pass
                testNonRestricted(exports);

                // access to opened pkg should pass
                opens.ifPresent(Test::testNonRestricted);

                // access to concealed pkg should fail
                testRestricted(conceals);

                // access to qualified export pkg should fail
                qualExports.ifPresent(Test::testRestricted);

                // access to qualified open pkg should fail
                qualOpens.ifPresent(Test::testRestricted);

                // access to qualified opened pkg that is also exported should pass
                qualOpensAndExports.ifPresent(Test::testNonRestricted);
            } else {
                System.out.println("Skipping tests for module.");
            }
        }

        private static void testRestricted(String pkg) {
            try {
                sm.checkPackageAccess(pkg);
                throw new RuntimeException("Able to access restricted package: "
                                           + pkg);
            } catch (SecurityException se) {}
            try {
                sm.checkPackageDefinition(pkg);
                throw new RuntimeException("Able to access restricted package: "
                                           + pkg);
            } catch (SecurityException se) {}
        }

        private static void testNonRestricted(String pkg) {
            try {
                sm.checkPackageAccess(pkg);
            } catch (SecurityException se) {
                throw new RuntimeException("Unable to access exported package: "
                                           + pkg, se);
            }
            try {
                sm.checkPackageDefinition(pkg);
            } catch (SecurityException se) {
                throw new RuntimeException("Unable to access exported package: "
                                           + pkg, se);
            }
        }
    }

    private static final Test[] tests = new Test[] {
        // java.base module loaded by boot loader
        new Test("java.base", "java.security", null, "jdk.internal.jrtfs",
                 "jdk.internal.loader", null, null),
        // java.desktop module loaded by boot loader and has an openQual pkg
        // that is exported
        new Test("java.desktop", "java.applet", null, "sun.font",
                 "sun.awt", null, "javax.swing.plaf.basic"),
        // java.security.jgss module loaded by platform loader
        new Test("java.security.jgss", "org.ietf.jgss", null,
                 "sun.security.krb5.internal.crypto", "sun.security.krb5",
                 null, null),
    };

    public static void main(String[] args) throws Exception {

        // check expected list of restricted packages in java.security file
        checkPackages(Arrays.asList(EXPECTED));

        // check access to each module's packages
        for (Test test : tests) {
            test.test();
        }

        System.out.println("Test passed");
    }

    private static void checkPackages(List<String> pkgs) {
        for (String pkg : pkgs) {
            try {
                sm.checkPackageAccess(pkg);
                throw new RuntimeException("Able to access " + pkg +
                                           " package");
            } catch (SecurityException se) { }
            try {
                sm.checkPackageDefinition(pkg);
                throw new RuntimeException("Able to define class in " + pkg +
                                           " package");
            } catch (SecurityException se) { }
            String subpkg = pkg + "foo";
            try {
                sm.checkPackageAccess(subpkg);
                throw new RuntimeException("Able to access " + subpkg +
                                           " package");
            } catch (SecurityException se) { }
            try {
                sm.checkPackageDefinition(subpkg);
                throw new RuntimeException("Able to define class in " +
                                           subpkg + " package");
            } catch (SecurityException se) { }
        }
    }
}
