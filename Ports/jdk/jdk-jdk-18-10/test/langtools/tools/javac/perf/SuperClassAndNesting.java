/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8263452
 * @summary Verify javac does not need a long time to process sources with deep class nesting
 *          and deep inheritance hierarchies.
 * @modules jdk.compiler
 */

import java.util.Arrays;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

public class SuperClassAndNesting {

    private static final int SIZE = 100;

    public static void main(String... args) throws IOException {
        new SuperClassAndNesting().run();
    }

    void run() throws IOException {
        compileTestClass(generateTestClass(SIZE));
    }

    String generateTestClass(int depth) {
        StringBuilder clazz = new StringBuilder();
        clazz.append("""
                     class Test {
                     class T0 extends java.util.ArrayList {
                     """);
        for (int i = 1; i < depth; i++) {
            clazz.append("class T" + i + " extends T" + (i - 1) + " {\n");
        }
        for (int i = 0; i < depth; i++) {
            clazz.append("}\n");
        }
        clazz.append("}\n");
        return clazz.toString();
    }

    void compileTestClass(String code) throws IOException {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        JavacTask ct = (JavacTask) tool.getTask(null, null, null,
            null, null, Arrays.asList(new MyFileObject(code)));
        ct.analyze();
    }

    static class MyFileObject extends SimpleJavaFileObject {
        private final String text;

        public MyFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }
}