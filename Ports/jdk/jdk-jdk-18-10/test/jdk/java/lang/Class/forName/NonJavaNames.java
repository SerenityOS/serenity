/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 4952558
 * @library /test/lib
 * @run testng/othervm NonJavaNames
 * @summary Verify names that aren't legal Java names are accepted by forName.
 */

public class NonJavaNames {
    public static class Baz {
        public Baz(){}
    }

    public static interface myInterface {
    }

     NonJavaNames.myInterface create() {
         // With target 1.5, this class's name will include a '+'
         // instead of a '$'.
         class Baz2 implements NonJavaNames.myInterface {
             public Baz2() { }
         }

        return new Baz2();
     }

    private static final String SRC_DIR = System.getProperty("test.src");
    private static final Path TEST_SRC = Path.of(SRC_DIR,  "classes");
    private static final Path TEST_CLASSES = Path.of(System.getProperty("test.classes", "."));

    @BeforeClass
    public void createInvalidNameClasses() throws IOException {
        Path hyphenPath = TEST_SRC.resolve("hyphen.class");
        Path commaPath = TEST_SRC.resolve("comma.class");
        Path periodPath = TEST_SRC.resolve("period.class");
        Path leftsquarePath = TEST_SRC.resolve("left-square.class");
        Path rightsquarePath = TEST_SRC.resolve("right-square.class");
        Path plusPath = TEST_SRC.resolve("plus.class");
        Path semicolonPath = TEST_SRC.resolve("semicolon.class");
        Path zeroPath = TEST_SRC.resolve("0.class");
        Path threePath = TEST_SRC.resolve("3.class");
        Path zadePath = TEST_SRC.resolve("Z.class");

        Path dhyphenPath = TEST_CLASSES.resolve("-.class");
        Path dcommaPath = TEST_CLASSES.resolve(",.class");
        Path dperiodPath = TEST_CLASSES.resolve("..class");
        Path dleftsquarePath = TEST_CLASSES.resolve("[.class");
        Path drightsquarePath = TEST_CLASSES.resolve("].class");
        Path dplusPath = TEST_CLASSES.resolve("+.class");
        Path dsemicolonPath = TEST_CLASSES.resolve(";.class");
        Path dzeroPath = TEST_CLASSES.resolve("0.class");
        Path dthreePath = TEST_CLASSES.resolve("3.class");
        Path dzadePath = TEST_CLASSES.resolve("Z.class");

        Files.copy(hyphenPath, dhyphenPath, REPLACE_EXISTING);
        Files.copy(commaPath, dcommaPath, REPLACE_EXISTING);
        Files.copy(periodPath, dperiodPath, REPLACE_EXISTING);
        Files.copy(leftsquarePath, dleftsquarePath, REPLACE_EXISTING);
        Files.copy(rightsquarePath, drightsquarePath, REPLACE_EXISTING);
        Files.copy(plusPath, dplusPath, REPLACE_EXISTING);
        Files.copy(semicolonPath, dsemicolonPath, REPLACE_EXISTING);
        Files.copy(zeroPath, dzeroPath, REPLACE_EXISTING);
        Files.copy(threePath, dthreePath, REPLACE_EXISTING);
        Files.copy(zadePath, dzadePath, REPLACE_EXISTING);
    }

    @Test
    public void testForNameReturnsSameClass() throws ClassNotFoundException {
        NonJavaNames.Baz bz = new NonJavaNames.Baz();
        String name;

        if (Class.forName(name=bz.getClass().getName()) != NonJavaNames.Baz.class) {
            System.err.println("Class object from forName does not match object.class.");
            System.err.println("Failures for class ``" + name + "''.");
            throw new RuntimeException();
        }

        NonJavaNames.myInterface bz2 = (new NonJavaNames()).create();
        if (Class.forName(name=bz2.getClass().getName()) != bz2.getClass()) {
            System.err.println("Class object from forName does not match getClass.");
            System.err.println("Failures for class ``" + name + "''.");
            throw new RuntimeException();
        }
    }

    @Test(dataProvider = "goodNonJavaClassNames")
    public void testGoodNonJavaClassNames(String name) throws ClassNotFoundException {
        System.out.println("Testing good class name ``" + name + "''");
        Class.forName(name);
    }

    @Test(dataProvider = "badNonJavaClassNames")
    public void testBadNonJavaClassNames(String name) {
        System.out.println("Testing bad class name ``" + name + "''");
        try {
            Class.forName(name);
        } catch (ClassNotFoundException e) {
            // Expected behavior
            return;
        }
        throw new RuntimeException("Bad class name ``" + name + "'' accepted.");
    }

    @DataProvider(name = "goodNonJavaClassNames")
    Object[][] getGoodNonJavaClassNames() {
        return new Object[][] {
                {","},
                {"+"},
                {"-"},
                {"0"},
                {"3"},
                // ":", These names won't work under windows.
                // "<",
                // ">",
                {"Z"},
                {"]"}
        };
    }

    @DataProvider(name = "badNonJavaClassNames")
    Object[][] getBadNonJavaClassNames() {
        return new Object[][] {
                {";"},
                {"["},
                {"."}
        };
    }
}
