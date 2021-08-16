/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8025091
 * @summary Tests the basic selection of FX related property methods, fields,
 *          setters and getters, by executing this test in the strict mode.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.* propgen.PropGen
 * @run main TestFxProperties
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javadoc.tester.JavadocTester;

public class TestFxProperties extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestFxProperties tester = new TestFxProperties();
        if (!tester.sanity()) {
            return;
        }
        tester.runTests();
    }

    // Check if FX modules are available.
    boolean sanity() {
        ClassLoader cl = this.getClass().getClassLoader();
        try {
            cl.loadClass("javafx.beans.Observable");
        } catch (ClassNotFoundException cnfe) {
            System.out.println("Note: javafx.beans.Observable: not found, test passes vacuously");
            return false;
        }
        return true;
    }

    @Test
    public void test1() throws Exception {
        Path srcdir = Paths.get("src-propgen");
        Files.createDirectory(srcdir);
        new propgen.PropGen(srcdir).run();
        Path srcfile = Paths.get(srcdir.toString(), "Demo.java");

        javadoc("-d", "out-propgen",
                "--javafx",
                srcfile.toString());
        checkExit(Exit.OK);
        checkOrder("Demo.html",
                "PROPERTY SUMMARY",
                "Property for fgp.",
                "Property for fgsp.",
                "Property for fp.",
                "Property for fsp.",
                "Property for gp.",
                "Property for gsp.",
                "Property for p.",
                "Property for sp.",
                "FIELD SUMMARY",
                "Field for f.",
                "Field for fg.",
                "Field for fgp.",
                "Field for fgs.",
                "Field for fgsp.",
                "Field for fp.",
                "Field for fs.",
                "Field for fsp.",
                "CONSTRUCTOR SUMMARY"
        );

        checkOrder("Demo.html",
                "METHOD SUMMARY",
                "Getter for fg.",
                "Getter for fgp.",
                "Getter for fgs.",
                "Getter for fgsp.",
                "Getter for g.",
                "Getter for gp.",
                "Getter for gs.",
                "Getter for gsp.",
                "Setter for fgs.",
                "Setter for fgsp.",
                "Setter for fs.",
                "Setter for fsp.",
                "Setter for gs.",
                "Setter for gsp.",
                "Setter for s.",
                "Setter for sp.",
                "Methods inherited");
    }
}
