/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @build m/*
 * @run main/othervm --add-modules m test.GetPackagesTest
 * @summary test the packages returned by Module::getPackages for an unnamed
 *          module does not include the packages for named modules
 */

package test;

import java.util.Set;
import static jdk.test.lib.Asserts.*;

public class GetPackagesTest {
    public static void main(String... args) throws Exception {
        // module m contains the package "p"
        Class<?> c = Class.forName("p.Main");
        Module m = c.getModule();
        Module test = GetPackagesTest.class.getModule();

        // module m and unnamed module are defined by the same class loader
        assertTrue(m.isNamed());
        assertFalse(test.isNamed());
        assertTrue(m.getClassLoader() == test.getClassLoader());

        // verify Module::getPackages on an unnamed module only contains
        // the packages defined to the unnamed module
        assertEquals(m.getPackages(), Set.of("p"));

        Set<String> pkgs = test.getPackages();
        assertTrue(pkgs.contains("test"));
        assertFalse(pkgs.contains("p"));
    }
}
