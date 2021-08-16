/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.ClassDefinition;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.helpers.ClassFileInstaller;

/*
 * Helper class to write tests that redefine classes.
 * When main method is run, it will create a redefineagent.jar that can be used
 * with the -javaagent option to support redefining classes in jtreg tests.
 *
 * See sample test in test/testlibrary_tests/RedefineClassTest.java
 */
public class RedefineClassHelper {

    public static Instrumentation instrumentation;
    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
    }

    /**
     * Redefine a class
     *
     * @param clazz Class to redefine
     * @param javacode String with the new java code for the class to be redefined
     */
    public static void redefineClass(Class clazz, String javacode) throws Exception {
        byte[] bytecode = InMemoryJavaCompiler.compile(clazz.getName(), javacode);
        redefineClass(clazz, bytecode);
    }

    /**
     * Redefine a class
     *
     * @param clazz Class to redefine
     * @param bytecode byte[] with the new class
     */
    public static void redefineClass(Class clazz, byte[] bytecode) throws Exception {
        instrumentation.redefineClasses(new ClassDefinition(clazz, bytecode));
    }

    /**
     * Main method to be invoked before test to create the redefineagent.jar
     */
    public static void main(String[] args) throws Exception {
        String manifest = "Premain-Class: RedefineClassHelper\nCan-Redefine-Classes: true\n";
        ClassFileInstaller.writeJar("redefineagent.jar", ClassFileInstaller.Manifest.fromString(manifest), "RedefineClassHelper");
    }
}
