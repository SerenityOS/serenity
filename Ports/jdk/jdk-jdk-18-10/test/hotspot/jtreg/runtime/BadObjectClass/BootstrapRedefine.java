/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6583051
 * @summary Give error if java.lang.Object has been incompatibly overridden on the bootpath
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver BootstrapRedefine
 */

import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class BootstrapRedefine {

    public static void main(String[] args) throws Exception {
        String source = "package java.lang;" +
                        "public class Object {" +
                        "    void dummy1() { return; }" +
                        "    void dummy2() { return; }" +
                        "    void dummy3() { return; }" +
                        "}";

        ClassFileInstaller.writeClassToDisk("java/lang/Object",
                                        InMemoryJavaCompiler.compile("java.lang.Object", source,
                                        "--patch-module=java.base"),
                                        "mods/java.base");

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("--patch-module=java.base=mods/java.base", "-version");
        new OutputAnalyzer(pb.start())
            .shouldContain("Incompatible definition of java.lang.Object")
            .shouldHaveExitValue(1);
    }
}
