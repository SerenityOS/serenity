/*
 * Copyright (c) 2018, Google Inc. All rights reserved.
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
 * @bug 8190452
 * @summary javac should not add MethodParameters attributes to v51 and earlier class files
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @build LegacyOutputTest
 * @run main LegacyOutputTest
 */

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.MethodParameters_attribute;
import com.sun.tools.classfile.MethodParameters_attribute.Entry;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Post https://bugs.openjdk.java.net/browse/JDK-8190452, the test verifies that MethodParameters
 * attributes are not emitted when targeting --release < 8.
 */
public class LegacyOutputTest {
    public static void main(String[] args) throws Exception {
        new LegacyOutputTest().test();
    }

    void test() throws Exception {
        release7();
        release8();
    }

    void release7() throws Exception {
        List<String> names = getParameterNames("7");
        if (names != null) {
            throw new AssertionError(
                    "expected no MethodParameters for --release 7, actual: " + names);
        }
    }

    void release8() throws Exception {
        List<String> names = getParameterNames("8");
        List<String> expected = Arrays.asList("x", "y");
        if (!names.equals(expected)) {
            throw new AssertionError(
                    "incorrect parameter names, actual: " + names + ", expected: " + expected);
        }
    }

    List<String> getParameterNames(String release) throws Exception {
        JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        JavaFileObject fileObject =
                new SimpleJavaFileObject(URI.create("Test.java"), Kind.SOURCE) {
                    @Override
                    public CharSequence getCharContent(boolean ignoreEncodingErrors)
                            throws IOException {
                        return "class Test { void f(int x, int y) {} }";
                    }
                };
        CompilationTask task =
                tool.getTask(
                        null,
                        null,
                        null,
                        Arrays.asList("--release", release, "-parameters"),
                        null,
                        Arrays.asList(fileObject));
        if (!task.call()) {
            throw new AssertionError("compilation failed");
        }
        ClassFile classFile = ClassFile.read(Paths.get("Test.class"));
        Method method = getMethod(classFile, "f");
        MethodParameters_attribute attribute =
                (MethodParameters_attribute) method.attributes.get("MethodParameters");
        if (attribute == null) {
            return null;
        }
        List<String> parameterNames = new ArrayList<>();
        for (Entry e : attribute.method_parameter_table) {
            parameterNames.add(classFile.constant_pool.getUTF8Value(e.name_index));
        }
        return parameterNames;
    }

    private static Method getMethod(ClassFile classFile, String name) throws Exception {
        for (Method method : classFile.methods) {
            if (classFile.constant_pool.getUTF8Value(method.name_index).equals(name)) {
                return method;
            }
        }
        throw new AssertionError("could not find method: " + name);
    }
}
