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

package jdk.test.lib.jittester;

import java.io.IOException;
import java.util.function.Function;
import jdk.test.lib.jittester.visitors.JavaCodeVisitor;

/**
 * Generates java source code from IRTree
 */
public class JavaCodeGenerator extends TestsGenerator {
    private static final String DEFAULT_SUFFIX = "java_tests";

    JavaCodeGenerator() {
        this(DEFAULT_SUFFIX, JavaCodeGenerator::generatePrerunAction, "-Xcomp");
    }

    JavaCodeGenerator(String prefix, Function<String, String[]> preRunActions, String jtDriverOptions) {
        super(prefix, preRunActions, jtDriverOptions);
    }

    @Override
    public void accept(IRNode mainClass, IRNode privateClasses) {
        String mainClassName = mainClass.getName();
        generateSources(mainClass, privateClasses);
        compilePrinter();
        compileJavaFile(mainClassName);
        generateGoldenOut(mainClassName);
    }

    private void generateSources(IRNode mainClass, IRNode privateClasses) {
        String mainClassName = mainClass.getName();
        StringBuilder code = new StringBuilder();
        JavaCodeVisitor vis = new JavaCodeVisitor();
        code.append(getJtregHeader(mainClassName));
        if (privateClasses != null) {
            code.append(privateClasses.accept(vis));
        }
        code.append(mainClass.accept(vis));
        ensureExisting(generatorDir);
        writeFile(generatorDir, mainClassName + ".java", code.toString());
    }

    private void compileJavaFile(String mainClassName) {
        String classPath = tmpDir.toString();
        ProcessBuilder pb = new ProcessBuilder(JAVAC,
                "-d", classPath,
                "-cp", classPath,
                generatorDir.resolve(mainClassName + ".java").toString());
        try {
            int r = runProcess(pb, tmpDir.resolve(mainClassName + ".javac").toString());
            if (r != 0) {
                throw new Error("Can't compile sources, exit code = " + r);
            }
        } catch (IOException | InterruptedException e) {
            throw new Error("Can't compile sources ", e);
        }
    }

    private static String[] generatePrerunAction(String mainClassName) {
        return new String[] {"@compile " + mainClassName + ".java"};
    }
}
