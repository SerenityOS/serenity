/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package build.tools.depend;

import com.sun.source.util.Plugin;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;


public class DependTest {

    public static void main(String... args) throws Exception {
        DependTest test = new DependTest();

        test.setupClass();

        test.testMethods();
        test.testFields();
        test.testModules();
        test.testAnnotations();
        test.testRecords();
    }

    public void testMethods() throws Exception {
        doOrdinaryTest("package test;" +
                       "public class Test {\n" +
                       "}",
                       "package test;" +
                       "public class Test {\n" +
                       "    public void test() {\n" +
                       "    }\n" +
                       "}",
                       true);
        doOrdinaryTest("package test;" +
                       "public class Test {\n" +
                       "}",
                       "package test;" +
                       "public class Test {\n" +
                       "    private void test() {\n" +
                       "    }\n" +
                       "}",
                       false);
        doOrdinaryTest("package test;" +
                       "public class Test {\n" +
                       "    public void test() {\n" +
                       "    }\n" +
                       "}",
                       "package test;" +
                       "public class Test {\n" +
                       "}",
                       true);
        doOrdinaryTest("package test;" +
                       "public class Test {\n" +
                       "    private void test() {\n" +
                       "    }\n" +
                       "}",
                       "package test;" +
                       "public class Test {\n" +
                       "}",
                       false);
    }

    public void testFields() throws Exception {
        doOrdinaryTest("package test;" +
                       "public class Test {\n" +
                       "}",
                       "package test;" +
                       "public class Test {\n" +
                       "    public int test;\n" +
                       "}",
                       true);
        doOrdinaryTest("package test;" +
                       "public class Test {\n" +
                       "}",
                       "package test;" +
                       "public class Test {\n" +
                       "    private int test;\n" +
                       "}",
                       false);
        doOrdinaryTest("package test;" +
                       "public class Test {\n" +
                       "    public static final int test = 0;\n" +
                       "}",
                       "package test;" +
                       "public class Test {\n" +
                       "    public static final int test = 1;\n" +
                       "}",
                       true);
        doOrdinaryTest("package test;" +
                       "public class Test {\n" +
                       "    public int test;\n" +
                       "}",
                       "package test;" +
                       "public class Test {\n" +
                       "}",
                       true);
        doOrdinaryTest("package test;" +
                       "public class Test {\n" +
                       "    private int test;\n" +
                       "}",
                       "package test;" +
                       "public class Test {\n" +
                       "}",
                       false);
    }

    public void testAnnotations() throws Exception {
        doOrdinaryTest("package test;" +
                       "public class Test {\n" +
                       "}",
                       "package test;" +
                       "@SuppressWarnings(\"any\")\n" +
                       "public class Test {\n" +
                       "}",
                       false);
        doOrdinaryTest("package test;" +
                       "public class Test {\n" +
                       "}",
                       "package test;" +
                       "@Deprecated\n" +
                       "public class Test {\n" +
                       "}",
                       true);
    }

    public void testModules() throws Exception {
        doModuleTest("module m { }",
                     "module m { requires java.compiler; }",
                     true);
        doModuleTest("module m { requires java.compiler; }",
                     "module m { requires java.compiler; }",
                     false);
        doModuleTest("module m { requires java.compiler; }",
                     "module m { requires jdk.compiler; }",
                     true);
        doModuleTest("module m { }",
                     "module m { exports test; }",
                     true);
        doModuleTest("module m { }",
                     "module m { exports test to java.base; }",
                     true);
        doModuleTest("module m { }",
                     "module m { exports test to java.compiler; }",
                     true);
        doModuleTest("module m { }",
                     "module m { uses test.Test1; }",
                     true);
        doModuleTest("module m { uses test.Test1; }",
                     "module m { uses test.Test2; }",
                     true);
        doModuleTest("module m { }",
                     "module m { provides test.Test1 with test.TestImpl1; }",
                     true);
        doModuleTest("module m { provides test.Test1 with test.TestImpl1; }",
                     "module m { provides test.Test2 with test.TestImpl1; }",
                     true);
        doModuleTest("module m { provides test.Test1 with test.TestImpl1; }",
                     "module m { provides test.Test1 with test.TestImpl2; }",
                     true);
    }

    public void testRecords() throws Exception {
        doOrdinaryTest("package test; public record Test (int x, int y) { }",
                       "package test; public record Test (int x, int y) { }",  // identical
                       false);
        doOrdinaryTest("package test; public record Test (int x, int y) { }",
                       "package test; public record Test (int x, int y) {" +
                               "public Test { } }",  // compact ctr
                       false);
        doOrdinaryTest("package test; public record Test (int x, int y) { }",
                       "package test; public record Test (int x, int y) {" +
                               "public Test (int x, int y) { this.x=x; this.y=y;} }",  // canonical ctr
                       false);
        doOrdinaryTest("package test; public record Test (int x, int y) { }",
                       "package test; public record Test (int y, int x) { }",  // reverse
                       true);
        doOrdinaryTest("package test; public record Test (int x, int y) { }",
                       "package test; public record Test (int x, int y, int z) { }", // additional
                       true);
        doOrdinaryTest("package test; public record Test (int x, int y) { }",
                       "package test; public record Test () { }", // empty
                       true);
        doOrdinaryTest("package test; public record Test (int x, int y) { }",
                       "package test; /*package*/ record Test (int x, int y) { }",  // package
                       true);
        doOrdinaryTest("package test; public record Test (int x, int y) { }",
                       "package test; public record Test (int x, int y) {" +
                               "public Test (int x, int y, int z) { this(x, y); } }",  // additional ctr
                       true);
    }

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private Path depend;
    private Path scratchServices;
    private Path scratchClasses;
    private Path apiHash;

    private void setupClass() throws IOException {
        depend = Paths.get(Depend.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        Path scratch = Files.createTempDirectory("depend-test");
        scratchServices = scratch.resolve("services");
        Path scratchClassesServices = scratchServices.resolve("META-INF").resolve("services");
        Files.createDirectories(scratchClassesServices);

        try (OutputStream out = Files.newOutputStream(scratchClassesServices.resolve(Plugin.class.getName()))) {
            out.write(Depend.class.getName().getBytes());
        }

        scratchClasses = scratch.resolve("classes");

        Files.createDirectories(scratchClasses);

        apiHash = scratch.resolve("api");
    }

    private void doOrdinaryTest(String codeBefore, String codeAfter, boolean hashChangeExpected) throws Exception {
        List<String> options =
                Arrays.asList("-d", scratchClasses.toString(),
                              "-processorpath", depend.toString() + File.pathSeparator + scratchServices.toString(),
                              "-Xplugin:depend " + apiHash.toString());
        List<TestJavaFileObject> beforeFiles =
                Arrays.asList(new TestJavaFileObject("module-info", "module m { exports test; }"),
                              new TestJavaFileObject("test.Test", codeBefore));
        compiler.getTask(null, null, null, options, null, beforeFiles).call();
        byte[] originalHash = Files.readAllBytes(apiHash);
        List<TestJavaFileObject> afterFiles =
                Arrays.asList(new TestJavaFileObject("module-info", "module m { exports test; }"),
                              new TestJavaFileObject("test.Test", codeAfter));
        compiler.getTask(null, null, null, options, null, afterFiles).call();
        byte[] newHash = Files.readAllBytes(apiHash);

        if (Arrays.equals(originalHash, newHash) ^ !hashChangeExpected) {
            throw new AssertionError("Unexpected hash state.");
        }
    }

    private void doModuleTest(String codeBefore, String codeAfter, boolean hashChangeExpected) throws Exception {
        List<String> options =
                Arrays.asList("-d", scratchClasses.toString(),
                              "-processorpath", depend.toString() + File.pathSeparator + scratchServices.toString(),
                              "-Xplugin:depend " + apiHash.toString());
        List<TestJavaFileObject> beforeFiles =
                Arrays.asList(new TestJavaFileObject("module-info", codeBefore),
                              new TestJavaFileObject("test.Test1", "package test; public interface Test1 {}"),
                              new TestJavaFileObject("test.Test2", "package test; public interface Test2 {}"),
                              new TestJavaFileObject("test.TestImpl1", "package test; public class TestImpl1 implements Test1, Test2 {}"),
                              new TestJavaFileObject("test.TestImpl2", "package test; public class TestImpl2 implements Test1, Test2 {}"));
        compiler.getTask(null, null, null, options, null, beforeFiles).call();
        byte[] originalHash = Files.readAllBytes(apiHash);
        List<TestJavaFileObject> afterFiles =
                Arrays.asList(new TestJavaFileObject("module-info", codeAfter),
                              new TestJavaFileObject("test.Test1", "package test; public interface Test1 {}"),
                              new TestJavaFileObject("test.Test2", "package test; public interface Test2 {}"),
                              new TestJavaFileObject("test.TestImpl1", "package test; public class TestImpl1 implements Test1, Test2 {}"),
                              new TestJavaFileObject("test.TestImpl2", "package test; public class TestImpl2 implements Test1, Test2 {}"));
        compiler.getTask(null, null, null, options, null, afterFiles).call();
        byte[] newHash = Files.readAllBytes(apiHash);

        if (Arrays.equals(originalHash, newHash) ^ !hashChangeExpected) {
            throw new AssertionError("Unexpected hash state.");
        }
    }

    private static final class TestJavaFileObject extends SimpleJavaFileObject {

        private final String code;

        public TestJavaFileObject(String className, String code) throws URISyntaxException {
            super(new URI("mem:/" + className.replace('.', '/') + ".java"), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean arg0) throws IOException {
            return code;
        }

    }
}
