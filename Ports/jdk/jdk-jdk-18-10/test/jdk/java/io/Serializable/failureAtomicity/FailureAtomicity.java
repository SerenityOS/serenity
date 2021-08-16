/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8071474
 * @summary Better failure atomicity for default read object.
 * @modules jdk.compiler
 * @library /test/lib
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 * @compile FailureAtomicity.java SerialRef.java
 * @run main failureAtomicity.FailureAtomicity
 */

package failureAtomicity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import jdk.test.lib.util.FileUtils;

@SuppressWarnings("unchecked")
public class FailureAtomicity {
    static final Path TEST_SRC = Paths.get(System.getProperty("test.src", "."));
    static final Path TEST_CLASSES = Paths.get(System.getProperty("test.classes", "."));
    static final Path fooTemplate = TEST_SRC.resolve("Foo.template");
    static final Path barTemplate = TEST_SRC.resolve("Bar.template");

    static final String[] PKGS = { "a.b.c", "x.y.z" };

    public static void main(String[] args) throws Exception {
        test_Foo();
        test_BadFoo();  // 'Bad' => incompatible type; cannot be "fully" deserialized
        test_FooWithReadObject();
        test_BadFooWithReadObject();

        test_Foo_Bar();
        test_Foo_BadBar();
        test_BadFoo_Bar();
        test_BadFoo_BadBar();
        test_Foo_BarWithReadObject();
        test_Foo_BadBarWithReadObject();
        test_BadFoo_BarWithReadObject();
        test_BadFoo_BadBarWithReadObject();
        test_FooWithReadObject_Bar();
        test_FooWithReadObject_BadBar();
        test_BadFooWithReadObject_Bar();
        test_BadFooWithReadObject_BadBar();
    }

    static final BiConsumer<Object,Object> FOO_FIELDS_EQUAL = (a,b) -> {
        try {
            int aPrim = a.getClass().getField("fooPrim").getInt(a);
            int bPrim = b.getClass().getField("fooPrim").getInt(b);
            if (aPrim != bPrim)
                throw new AssertionError("Not equal: (" + aPrim + "!=" + bPrim
                                         + "), in [" + a + "] [" + b + "]");
            Object aRef = a.getClass().getField("fooRef").get(a);
            Object bRef = b.getClass().getField("fooRef").get(b);
            if (!aRef.equals(bRef))
                throw new RuntimeException("Not equal: (" + aRef + "!=" + bRef
                                           + "), in [" + a + "] [" + b + "]");
        } catch (NoSuchFieldException | IllegalAccessException x) {
            throw new InternalError(x);
        }
    };
    static final BiConsumer<Object,Object> FOO_FIELDS_DEFAULT = (ignore,b) -> {
        try {
            int aPrim = b.getClass().getField("fooPrim").getInt(b);
            if (aPrim != 0)
                throw new AssertionError("Expected 0, got:" + aPrim
                                         + ", in [" + b + "]");
            Object aRef = b.getClass().getField("fooRef").get(b);
            if (aRef != null)
                throw new RuntimeException("Expected null, got:" + aRef
                                           + ", in [" + b + "]");
        } catch (NoSuchFieldException | IllegalAccessException x) {
            throw new InternalError(x);
        }
    };
    static final BiConsumer<Object,Object> BAR_FIELDS_EQUAL = (a,b) -> {
        try {
            long aPrim = a.getClass().getField("barPrim").getLong(a);
            long bPrim = b.getClass().getField("barPrim").getLong(b);
            if (aPrim != bPrim)
                throw new AssertionError("Not equal: (" + aPrim + "!=" + bPrim
                                         + "), in [" + a + "] [" + b + "]");
            Object aRef = a.getClass().getField("barRef").get(a);
            Object bRef = b.getClass().getField("barRef").get(b);
            if (!aRef.equals(bRef))
                throw new RuntimeException("Not equal: (" + aRef + "!=" + bRef
                                           + "), in [" + a + "] [" + b + "]");
        } catch (NoSuchFieldException | IllegalAccessException x) {
            throw new InternalError(x);
        }
    };
    static final BiConsumer<Object,Object> BAR_FIELDS_DEFAULT = (ignore,b) -> {
        try {
            long aPrim = b.getClass().getField("barPrim").getLong(b);
            if (aPrim != 0L)
                throw new AssertionError("Expected 0, got:" + aPrim
                                         + ", in [" + b + "]");
            Object aRef = b.getClass().getField("barRef").get(b);
            if (aRef != null)
                throw new RuntimeException("Expected null, got:" + aRef
                                           + ", in [" + b + "]");
        } catch (NoSuchFieldException | IllegalAccessException x) {
            throw new InternalError(x);
        }
    };

    static void test_Foo() {
        testFoo("Foo", "String", false, false, FOO_FIELDS_EQUAL); }
    static void test_BadFoo() {
        testFoo("BadFoo", "byte[]", true, false, FOO_FIELDS_DEFAULT); }
    static void test_FooWithReadObject() {
        testFoo("FooWithReadObject", "String", false, true, FOO_FIELDS_EQUAL); }
    static void test_BadFooWithReadObject() {
        testFoo("BadFooWithReadObject", "byte[]", true, true, FOO_FIELDS_DEFAULT); }

    static void testFoo(String testName, String xyzZebraType,
                        boolean expectCCE, boolean withReadObject,
                        BiConsumer<Object,Object>... resultCheckers) {
        System.out.println("\nTesting " + testName);
        try {
            Path testRoot = testDir(testName);
            Path srcRoot = Files.createDirectory(testRoot.resolve("src"));
            List<Path> srcFiles = new ArrayList<>();
            srcFiles.add(createSrc(PKGS[0], fooTemplate, srcRoot, "String", withReadObject));
            srcFiles.add(createSrc(PKGS[1], fooTemplate, srcRoot, xyzZebraType, withReadObject));

            Path build = Files.createDirectory(testRoot.resolve("build"));
            javac(build, srcFiles);

            URLClassLoader loader = new URLClassLoader(new URL[]{ build.toUri().toURL() },
                                                       FailureAtomicity.class.getClassLoader());
            Class<?> fooClass = Class.forName(PKGS[0] + ".Foo", true, loader);
            Constructor<?> ctr = fooClass.getConstructor(
                    new Class<?>[]{int.class, String.class, String.class});
            Object abcFoo = ctr.newInstance(5, "chegar", "zebra");

            try {
                toOtherPkgInstance(abcFoo, loader);
                if (expectCCE)
                    throw new AssertionError("Expected CCE not thrown");
            } catch (ClassCastException e) {
                if (!expectCCE)
                    throw new AssertionError("UnExpected CCE: " + e);
            }

            Object deserialInstance = failureAtomicity.SerialRef.obj;

            System.out.println("abcFoo:           " + abcFoo);
            System.out.println("deserialInstance: " + deserialInstance);

            for (BiConsumer<Object, Object> rc : resultCheckers)
                rc.accept(abcFoo, deserialInstance);
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        } catch (ReflectiveOperationException x) {
            throw new InternalError(x);
        }
    }

    static void test_Foo_Bar() {
        testFooBar("Foo_Bar", "String", "String", false, false, false,
                   FOO_FIELDS_EQUAL, BAR_FIELDS_EQUAL);
    }
    static void test_Foo_BadBar() {
        testFooBar("Foo_BadBar", "String", "byte[]", true, false, false,
                   FOO_FIELDS_DEFAULT, BAR_FIELDS_DEFAULT);
    }
    static void test_BadFoo_Bar() {
        testFooBar("BadFoo_Bar", "byte[]", "String", true, false, false,
                   FOO_FIELDS_DEFAULT, BAR_FIELDS_DEFAULT);
    }
    static void test_BadFoo_BadBar() {
        testFooBar("BadFoo_BadBar", "byte[]", "byte[]", true, false, false,
                   FOO_FIELDS_DEFAULT, BAR_FIELDS_DEFAULT);
    }
    static void test_Foo_BarWithReadObject() {
        testFooBar("Foo_BarWithReadObject", "String", "String", false, false, true,
                   FOO_FIELDS_EQUAL, BAR_FIELDS_EQUAL);
    }
    static void test_Foo_BadBarWithReadObject() {
        testFooBar("Foo_BadBarWithReadObject", "String", "byte[]", true, false, true,
                   FOO_FIELDS_EQUAL, BAR_FIELDS_DEFAULT);
    }
    static void test_BadFoo_BarWithReadObject() {
        testFooBar("BadFoo_BarWithReadObject", "byte[]", "String", true, false, true,
                   FOO_FIELDS_DEFAULT, BAR_FIELDS_DEFAULT);
    }
    static void test_BadFoo_BadBarWithReadObject() {
        testFooBar("BadFoo_BadBarWithReadObject", "byte[]", "byte[]", true, false, true,
                   FOO_FIELDS_DEFAULT, BAR_FIELDS_DEFAULT);
    }

    static void test_FooWithReadObject_Bar() {
        testFooBar("FooWithReadObject_Bar", "String", "String", false, true, false,
                   FOO_FIELDS_EQUAL, BAR_FIELDS_EQUAL);
    }
    static void test_FooWithReadObject_BadBar() {
        testFooBar("FooWithReadObject_BadBar", "String", "byte[]", true, true, false,
                   FOO_FIELDS_EQUAL, BAR_FIELDS_DEFAULT);
    }
    static void test_BadFooWithReadObject_Bar() {
        testFooBar("BadFooWithReadObject_Bar", "byte[]", "String", true, true, false,
                   FOO_FIELDS_DEFAULT, BAR_FIELDS_DEFAULT);
    }
    static void test_BadFooWithReadObject_BadBar() {
        testFooBar("BadFooWithReadObject_BadBar", "byte[]", "byte[]", true, true, false,
                   FOO_FIELDS_DEFAULT, BAR_FIELDS_DEFAULT);
    }

    static void testFooBar(String testName, String xyzFooZebraType,
                           String xyzBarZebraType, boolean expectCCE,
                           boolean fooWithReadObject, boolean barWithReadObject,
                           BiConsumer<Object,Object>... resultCheckers) {
        System.out.println("\nTesting " + testName);
        try {
            Path testRoot = testDir(testName);
            Path srcRoot = Files.createDirectory(testRoot.resolve("src"));
            List<Path> srcFiles = new ArrayList<>();
            srcFiles.add(createSrc(PKGS[0], fooTemplate, srcRoot, "String",
                                   fooWithReadObject, "String"));
            srcFiles.add(createSrc(PKGS[1], fooTemplate, srcRoot, xyzFooZebraType,
                                   fooWithReadObject, xyzFooZebraType));
            srcFiles.add(createSrc(PKGS[0], barTemplate, srcRoot, "String",
                                   barWithReadObject, "String"));
            srcFiles.add(createSrc(PKGS[1], barTemplate, srcRoot, xyzBarZebraType,
                                   barWithReadObject, xyzFooZebraType));

            Path build = Files.createDirectory(testRoot.resolve("build"));
            javac(build, srcFiles);

            URLClassLoader loader = new URLClassLoader(new URL[]{ build.toUri().toURL() },
                                                       FailureAtomicity.class.getClassLoader());
            Class<?> fooClass = Class.forName(PKGS[0] + ".Bar", true, loader);
            Constructor<?> ctr = fooClass.getConstructor(
                    new Class<?>[]{int.class, String.class, String.class,
                                   long.class, String.class, String.class});
            Object abcBar = ctr.newInstance( 5, "chegar", "zebraFoo", 111L, "aBar", "zebraBar");

            try {
                toOtherPkgInstance(abcBar, loader);
                if (expectCCE)
                    throw new AssertionError("Expected CCE not thrown");
            } catch (ClassCastException e) {
                if (!expectCCE)
                    throw new AssertionError("UnExpected CCE: " + e);
            }

            Object deserialInstance = failureAtomicity.SerialRef.obj;

            System.out.println("abcBar:           " + abcBar);
            System.out.println("deserialInstance: " + deserialInstance);

            for (BiConsumer<Object, Object> rc : resultCheckers)
                rc.accept(abcBar, deserialInstance);
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        } catch (ReflectiveOperationException x) {
            throw new InternalError(x);
        }
    }

    static Path testDir(String name) throws IOException {
        Path testRoot = Paths.get("FailureAtomicity-" + name);
        if (Files.exists(testRoot))
            FileUtils.deleteFileTreeWithRetry(testRoot);
        Files.createDirectory(testRoot);
        return testRoot;
    }

    static String platformPath(String p) { return p.replace("/", File.separator); }
    static String binaryName(String name) { return name.replace(".", "/"); }
    static String condRemove(String line, String pattern, boolean hasReadObject) {
        if (hasReadObject) { return line.replaceAll(pattern, ""); }
        else { return line; }
    }
    static String condReplace(String line, String... zebraFooType) {
        if (zebraFooType.length == 1) {
            return line.replaceAll("\\$foo_zebra_type", zebraFooType[0]);
        } else { return line; }
    }
    static String nameFromTemplate(Path template) {
        return template.getFileName().toString().replaceAll(".template", "");
    }

    static Path createSrc(String pkg, Path srcTemplate, Path srcRoot,
                          String zebraType, boolean hasReadObject,
                          String... zebraFooType)
        throws IOException
    {
        Path srcDst = srcRoot.resolve(platformPath(binaryName(pkg)));
        Files.createDirectories(srcDst);
        Path srcFile = srcDst.resolve(nameFromTemplate(srcTemplate) + ".java");

        List<String> lines = Files.lines(srcTemplate)
                .map(s -> s.replaceAll("\\$package", pkg))
                .map(s -> s.replaceAll("\\$zebra_type", zebraType))
                .map(s -> condReplace(s, zebraFooType))
                .map(s -> condRemove(s, "//\\$has_readObject", hasReadObject))
                .collect(Collectors.toList());
        Files.write(srcFile, lines);
        return srcFile;
    }

    static void javac(Path dest, List<Path> sourceFiles) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(null, null, null)) {
            List<File> files = sourceFiles.stream()
                                          .map(p -> p.toFile())
                                          .collect(Collectors.toList());
            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromFiles(files);
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT,
                                    Arrays.asList(dest.toFile()));
            fileManager.setLocation(StandardLocation.CLASS_PATH,
                                    Arrays.asList(TEST_CLASSES.toFile()));
            JavaCompiler.CompilationTask task = compiler
                    .getTask(null, fileManager, null, null, null, compilationUnits);
            boolean passed = task.call();
            if (!passed)
                throw new RuntimeException("Error compiling " + files);
        }
    }

    static Object toOtherPkgInstance(Object obj, ClassLoader loader)
        throws IOException, ClassNotFoundException
    {
        byte[] bytes = serialize(obj);
        bytes = replacePkg(bytes);
        return deserialize(bytes, loader);
    }

    @SuppressWarnings("deprecation")
    static byte[] replacePkg(byte[] bytes) {
        String str = new String(bytes, 0);
        str = str.replaceAll(PKGS[0], PKGS[1]);
        str.getBytes(0, bytes.length, bytes, 0);
        return bytes;
    }

    static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(baos);) {
            out.writeObject(obj);
            return baos.toByteArray();
        }
    }

    static Object deserialize(byte[] data, ClassLoader l)
        throws IOException, ClassNotFoundException
    {
        return new WithLoaderObjectInputStream(new ByteArrayInputStream(data), l)
                .readObject();
    }

    static class WithLoaderObjectInputStream extends ObjectInputStream {
        final ClassLoader loader;
        WithLoaderObjectInputStream(InputStream is, ClassLoader loader)
            throws IOException
        {
            super(is);
            this.loader = loader;
        }
        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {
            try {
                return super.resolveClass(desc);
            } catch (ClassNotFoundException x) {
                String name = desc.getName();
                return Class.forName(name, false, loader);
            }
        }
    }
}
