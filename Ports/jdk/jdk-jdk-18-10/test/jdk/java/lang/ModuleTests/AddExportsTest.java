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

/**
 * @test
 * @modules java.base/jdk.internal.misc
 *          java.desktop
 * @run main/othervm --add-exports=java.desktop/sun.awt=java.base AddExportsTest
 * @run main/othervm --add-exports=java.desktop/sun.awt=ALL-UNNAMED AddExportsTest
 * @summary Test Module isExported methods with exports changed by -AddExportsTest
 */

import java.util.Optional;
import java.util.stream.Stream;

import jdk.internal.misc.VM;

public class AddExportsTest {
    /*
     * jtreg sets -Dtest.modules system property to the internal APIs
     * specified at @modules tag.  The test will exclude --add-exports set
     * for @modules.
     */
    private static final String TEST_MODULES = System.getProperty("test.modules");

    public static void main(String[] args) {

        Optional<String> oaddExports = Stream.of(VM.getRuntimeArguments())
            .filter(arg -> arg.startsWith("--add-exports="))
            .filter(arg -> !arg.equals("--add-exports=" + TEST_MODULES + "=ALL-UNNAMED"))
            .map(arg -> arg.substring("--add-exports=".length(), arg.length()))
            .findFirst();

        assertTrue(oaddExports.isPresent());

        ModuleLayer bootLayer = ModuleLayer.boot();

        Module unnamedModule = AddExportsTest.class.getModule();
        assertFalse(unnamedModule.isNamed());

        for (String expr : oaddExports.get().split(",")) {

            String[] s = expr.split("=");
            assertTrue(s.length == 2);

            // $MODULE/$PACKAGE
            String[] moduleAndPackage = s[0].split("/");
            assertTrue(moduleAndPackage.length == 2);

            String mn = moduleAndPackage[0];
            String pn = moduleAndPackage[1];

            // source module
            Module source;
            Optional<Module> om = bootLayer.findModule(mn);
            assertTrue(om.isPresent(), mn + " not in boot layer");
            source = om.get();

            // package should not be exported unconditionally
            assertFalse(source.isExported(pn),
                        pn + " should not be exported unconditionally");

            // $TARGET
            String tn = s[1];
            if ("ALL-UNNAMED".equals(tn)) {

                // package is exported to all unnamed modules
                assertTrue(source.isExported(pn, unnamedModule),
                           pn + " should be exported to all unnamed modules");

            } else {

                om = bootLayer.findModule(tn);
                assertTrue(om.isPresent());
                Module target = om.get();

                // package should be exported to target module
                assertTrue(source.isExported(pn, target),
                           pn + " should be exported to " + target);

                // package should not be exported to unnamed modules
                assertFalse(source.isExported(pn, unnamedModule),
                            pn + " should not be exported to unnamed modules");

            }

        }
    }

    static void assertTrue(boolean cond) {
        if (!cond) throw new RuntimeException();
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new RuntimeException(msg);
    }

    static void assertFalse(boolean cond) {
        if (cond) throw new RuntimeException();
    }

    static void assertFalse(boolean cond, String msg) {
        if (cond) throw new RuntimeException(msg);
    }

}
