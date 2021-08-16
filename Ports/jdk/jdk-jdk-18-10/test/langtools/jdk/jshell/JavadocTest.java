/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8131019 8169561 8174245
 * @summary Test Javadoc
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jshell/jdk.jshell:open
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @build KullaTesting TestingInputStream Compiler
 * @run testng JavadocTest
 */

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.testng.annotations.Test;

@Test
public class JavadocTest extends KullaTesting {

    private final Compiler compiler = new Compiler();

    public void testJavadoc() {
        prepareZip();
        assertJavadoc("test.Clazz|", "test.Clazz\n" +
                                     "Top level. ");
        assertEval("test.Clazz clz = null;");
        assertJavadoc("clz.test(|", "String test.Clazz.test(int p) throws IllegalStateException\n" +
                                    " javadoc1A\n" +
                                    "\n" +
                                    " @param p param\n" +
                                    " @throws IllegalStateException exc\n" +
                                    " @return value\n");
        //undefined handling:
        assertJavadoc("clz.undef|");
    }

    public void testVariableInRepl() {
        assertEval("Object o;");
        assertSignature("o|", "o:java.lang.Object");
    }

    private void prepareZip() {
        String clazz =
                "package test;\n" +
                "/**Top level." +
                " */\n" +
                "public class Clazz {\n" +
                "    /**\n" +
                "     * javadoc1A\n" +
                "     *\n" +
                "     * @param p param\n" +
                "     * @throws IllegalStateException exc\n" +
                "     * @return value\n" +
                "     */\n" +
                "    public String test(int p) throws IllegalStateException { return null;}\n" +
                "}\n";

        Path srcZip = Paths.get("src.zip");

        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(srcZip))) {
            out.putNextEntry(new JarEntry("test/Clazz.java"));
            out.write(clazz.getBytes());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        compiler.compile(clazz);

        try {
            Field availableSources = getAnalysis().getClass().getDeclaredField("availableSources");
            availableSources.setAccessible(true);
            availableSources.set(getAnalysis(), Arrays.asList(srcZip));
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
        addToClasspath(compiler.getClassDir());
    }

    public void testCollectionsMin() {
        prepareJavaUtilZip();
        assertJavadoc("java.util.Collections.min(|",
                      "T java.util.Collections.<T>min(java.util.Collection<? extends T> coll, java.util.Comparator<? super T> comp)\n" +
                       " min comparator\n",
                       "T java.util.Collections.<T extends Object & Comparable<? super T>>min(java.util.Collection<? extends T> coll)\n" +
                       " min comparable\n");
    }

    private void prepareJavaUtilZip() {
        String clazz =
                "package java.util;\n" +
                "/**Top level." +
                " */\n" +
                "public class Collections {\n" +
                "    /**\n" +
                "     * min comparable\n" +
                "     */\n" +
                "    public static <T extends Object & Comparable<? super T>> T min(Collection<? extends T> coll) {" +
                "        return null;\n" +
                "    }\n" +
                "    /**\n" +
                "     * min comparator\n" +
                "     */\n" +
                "        public static <T> T min(Collection<? extends T> coll, Comparator<? super T> comp) {\n" +
                "        return null;\n" +
                "    }\n" +
                "}\n";

        Path srcZip = Paths.get("src.zip");

        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(srcZip))) {
            out.putNextEntry(new JarEntry("java/util/Collections.java"));
            out.write(clazz.getBytes());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        try {
            Field availableSources = getAnalysis().getClass().getDeclaredField("availableSources");
            availableSources.setAccessible(true);
            availableSources.set(getAnalysis(), Arrays.asList(srcZip));
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
