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
 * @bug 8208531
 * @summary -javafx mode should be on by default when JavaFX is available.
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestJavaFxMode
 */

import toolbox.ToolBox;

import java.nio.file.Path;
import java.nio.file.Paths;

import javadoc.tester.JavadocTester;

public class TestJavaFxMode extends JavadocTester {

    final ToolBox tb;

    public static void main(String... args) throws Exception {
        TestJavaFxMode tester = new TestJavaFxMode();
        if (tester.sanity()) {
            tester.runTests(m -> new Object[]{Paths.get(m.getName())});
        }
    }

    TestJavaFxMode() {
        tb = new ToolBox();
    }

    // Check if FX modules are available.
    boolean sanity() {
        try {
            Class.forName("javafx.beans.Observable");
        } catch (ClassNotFoundException cnfe) {
            System.out.println("Note: javafx.beans.Observable: not found, test passes vacuously");
            return false;
        }
        return true;
    }

    @Test
    public void test(Path base) throws Exception {
        Path src = base.resolve("src");
        createTestClass(src);
        Path outDir = base.resolve("out");

        javadoc("-d", outDir.toString(),
                "-sourcepath", src.toString(),
                "pkg");

        checkExit(Exit.OK);
        checkOrder("pkg/A.html",
                "Property Summary",
                "javafx.beans.property.Property", "<a href=\"#propProperty\">prop</a>",
                "Field Summary",
                "javafx.beans.property.Property", "<a href=\"#prop\">prop</a></span>",
                "Method Summary",
                "<a href=\"#getProp()\">getProp</a>", "Gets the value of the property prop.",
                """
                    <a href="#propProperty()">propProperty</a>""", "Sets the value of the property prop.");
    }

    void createTestClass(Path src) throws Exception {
        tb.writeJavaFiles(src,
                """
                    package pkg;
                    import javafx.beans.property.Property;
                    public class A {
                        public Property prop;
                        public Property propProperty(){return null;}
                        public Property getProp(){return null;}
                        public void setProp(Property prop){}
                    }""");
    }

}
