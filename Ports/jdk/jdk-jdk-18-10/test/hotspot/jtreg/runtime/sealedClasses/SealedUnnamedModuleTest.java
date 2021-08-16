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
 * @compile planets/OuterPlanets.jcod planets/Mars.jcod
 * @compile planets/Neptune.java asteroids/Pluto.java asteroids/Charon.java
 * @run main SealedUnnamedModuleTest
 */

public class SealedUnnamedModuleTest {

    public static void main(String args[]) throws Throwable {

        // Classes Neptune, Mars, Pluto, and Charon all try to extend sealed class OuterPlanets.
        // Class OuterPlanets permits Nepturn, Pluto, and Charon.

        // Test permitted subclass and superclass in unnamed module and same package.
        // This should succeed.
        Class neptune = Class.forName("planets.Neptune");

        // Test unpermitted subclass and superclass in unnamed module and same package.
        // This should fail.
        try {
            Class mars = Class.forName("planets.Mars");
            throw new RuntimeException("Expected IncompatibleClassChangeError exception not thrown");
        } catch (IncompatibleClassChangeError e) {
            if (!e.getMessage().contains("cannot inherit from sealed class")) {
                throw new RuntimeException("Wrong IncompatibleClassChangeError exception thrown: " + e.getMessage());
            }
        }

        // Test non-public permitted subclass and superclass in same unnamed module but
        // in different packages.  This should fail.
        try {
            Class pluto = Class.forName("asteroids.Pluto");
            throw new RuntimeException("Expected IncompatibleClassChangeError exception not thrown");
        } catch (IncompatibleClassChangeError e) {
            if (!e.getMessage().contains("cannot inherit from sealed class")) {
                throw new RuntimeException("Wrong IncompatibleClassChangeError exception thrown: " + e.getMessage());
            }
        }

        // Test public permitted subclass and superclass in same unnamed module but
        // in different packages.  This should succeed.
        Class charon = Class.forName("asteroids.Charon");
    }
}
