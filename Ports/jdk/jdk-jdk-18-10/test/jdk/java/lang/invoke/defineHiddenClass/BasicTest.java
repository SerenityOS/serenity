/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          jdk.compiler
 * @library /test/lib
 * @compile BadClassFile.jcod
 *          BadClassFile2.jcod
 *          BadClassFileVersion.jcod
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.compiler.CompilerUtils
 * @run testng/othervm BasicTest
 */

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles.Lookup;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Type;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.Utils;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static org.testng.Assert.*;

interface HiddenTest {
    void test();
}

public class BasicTest {

    private static final Path SRC_DIR = Paths.get(Utils.TEST_SRC, "src");
    private static final Path CLASSES_DIR = Paths.get("classes");
    private static final Path CLASSES_10_DIR = Paths.get("classes_10");

    private static byte[] hiddenClassBytes;

    @BeforeTest
    static void setup() throws IOException {
        compileSources(SRC_DIR, CLASSES_DIR);
        hiddenClassBytes = Files.readAllBytes(CLASSES_DIR.resolve("HiddenClass.class"));

        // compile with --release 10 with no NestHost and NestMembers attribute
        compileSources(SRC_DIR.resolve("Outer.java"), CLASSES_10_DIR, "--release", "10");
        compileSources(SRC_DIR.resolve("EnclosingClass.java"), CLASSES_10_DIR, "--release", "10");
    }

    static void compileSources(Path sourceFile, Path dest, String... options) throws IOException {
        Stream<String> ops = Stream.of("-cp", Utils.TEST_CLASSES + File.pathSeparator + CLASSES_DIR);
        if (options != null && options.length > 0) {
            ops = Stream.concat(ops, Arrays.stream(options));
        }
        if (!CompilerUtils.compile(sourceFile, dest, ops.toArray(String[]::new))) {
            throw new RuntimeException("Compilation of the test failed: " + sourceFile);
        }
    }

    static Class<?> defineHiddenClass(String name) throws Exception {
        byte[] bytes = Files.readAllBytes(CLASSES_DIR.resolve(name + ".class"));
        Class<?> hc = lookup().defineHiddenClass(bytes, false).lookupClass();
        assertHiddenClass(hc);
        singletonNest(hc);
        return hc;
    }

    // basic test on a hidden class
    @Test
    public void hiddenClass() throws Throwable {
        HiddenTest t = (HiddenTest)defineHiddenClass("HiddenClass").newInstance();
        t.test();

        // sanity check
        Class<?> c = t.getClass();
        Class<?>[] intfs = c.getInterfaces();
        assertTrue(c.isHidden());
        assertFalse(c.isPrimitive());
        assertTrue(intfs.length == 1);
        assertTrue(intfs[0] == HiddenTest.class);
        assertTrue(c.getCanonicalName() == null);

        String hcName = "HiddenClass";
        String hcSuffix = "0x[0-9a-f]+";
        assertTrue(c.getName().matches(hcName + "/" + hcSuffix));
        assertTrue(c.descriptorString().matches("L" + hcName + "." + hcSuffix + ";"), c.descriptorString());

        // test array of hidden class
        testHiddenArray(c);

        // test setAccessible
        checkSetAccessible(c, "realTest");
        checkSetAccessible(c, "test");
    }

    // primitive class is not a hidden class
    @Test
    public void primitiveClass() {
        assertFalse(int.class.isHidden());
        assertFalse(String.class.isHidden());
    }

    private void testHiddenArray(Class<?> type) throws Exception {
        // array of hidden class
        Object array = Array.newInstance(type, 2);
        Class<?> arrayType = array.getClass();
        assertTrue(arrayType.isArray());
        assertTrue(Array.getLength(array) == 2);
        assertFalse(arrayType.isHidden());

        String hcName = "HiddenClass";
        String hcSuffix = "0x[0-9a-f]+";
        assertTrue(arrayType.getName().matches("\\[" + "L" + hcName + "/" + hcSuffix + ";"));
        assertTrue(arrayType.descriptorString().matches("\\[" + "L" + hcName + "." + hcSuffix + ";"));

        assertTrue(arrayType.getComponentType().isHidden());
        assertTrue(arrayType.getComponentType() == type);
        Object t = type.newInstance();
        Array.set(array, 0, t);
        Object o = Array.get(array, 0);
        assertTrue(o == t);
    }

    private void checkSetAccessible(Class<?> c, String name, Class<?>... ptypes) throws Exception {
        Method m = c.getDeclaredMethod(name, ptypes);
        assertTrue(m.trySetAccessible());
        m.setAccessible(true);
    }

    // Define a hidden class that uses lambda
    // This verifies LambdaMetaFactory supports the caller which is a hidden class
    @Test
    public void testLambda() throws Throwable {
        HiddenTest t = (HiddenTest)defineHiddenClass("Lambda").newInstance();
        try {
            t.test();
        } catch (Error e) {
            if (!e.getMessage().equals("thrown by " + t.getClass().getName())) {
                throw e;
            }
        }
    }

    // Verify the nest host and nest members of a hidden class and hidden nestmate class
    @Test
    public void testHiddenNestHost() throws Throwable {
        byte[] hc1 = hiddenClassBytes;
        Lookup lookup1 = lookup().defineHiddenClass(hc1, false);
        Class<?> host = lookup1.lookupClass();

        byte[] hc2 = Files.readAllBytes(CLASSES_DIR.resolve("Lambda.class"));
        Lookup lookup2 = lookup1.defineHiddenClass(hc2, false, NESTMATE);
        Class<?> member = lookup2.lookupClass();

        // test nest membership and reflection API
        assertTrue(host.isNestmateOf(member));
        assertTrue(host.getNestHost() == host);
        // getNestHost and getNestMembers return the same value when calling
        // on a nest member and the nest host
        assertTrue(member.getNestHost() == host.getNestHost());
        assertTrue(Arrays.equals(member.getNestMembers(), host.getNestMembers()));
        // getNestMembers includes the nest host that can be a hidden class but
        // only includes static nest members
        assertTrue(host.getNestMembers().length == 1);
        assertTrue(host.getNestMembers()[0] == host);
    }

    @DataProvider(name = "hiddenClasses")
    private Object[][] hiddenClasses() {
        return new Object[][] {
                new Object[] { "HiddenInterface", false },
                new Object[] { "AbstractClass", false },
                // a hidden annotation is useless because it cannot be referenced by any class
                new Object[] { "HiddenAnnotation", false },
                // class file with bad NestHost, NestMembers and InnerClasses or EnclosingMethod attribute
                // define them as nestmate to verify Class::getNestHost and getNestMembers
                new Object[] { "Outer", true },
                new Object[] { "Outer$Inner", true },
                new Object[] { "EnclosingClass", true },
                new Object[] { "EnclosingClass$1", true },
        };
    }

    /*
     * Test that class file bytes that can be defined as a normal class
     * can be successfully created as a hidden class even it might not
     * make sense as a hidden class.  For example, a hidden annotation
     * is not useful as it cannot be referenced and an outer/inner class
     * when defined as a hidden effectively becomes a final top-level class.
     */
    @Test(dataProvider = "hiddenClasses")
    public void defineHiddenClass(String name, boolean nestmate) throws Exception {
        byte[] bytes = Files.readAllBytes(CLASSES_DIR.resolve(name + ".class"));
        Class<?> hc;
        Class<?> host;
        if (nestmate) {
            hc = lookup().defineHiddenClass(bytes, false, NESTMATE).lookupClass();
            host = lookup().lookupClass().getNestHost();
        } else {
            hc = lookup().defineHiddenClass(bytes, false).lookupClass();
            host = hc;
        }
        assertTrue(hc.getNestHost() == host);
        assertTrue(hc.getNestMembers().length == 1);
        assertTrue(hc.getNestMembers()[0] == host);
    }

    @DataProvider(name = "emptyClasses")
    private Object[][] emptyClasses() {
        return new Object[][] {
                new Object[] { "EmptyHiddenSynthetic", ACC_SYNTHETIC },
                new Object[] { "EmptyHiddenEnum", ACC_ENUM },
                new Object[] { "EmptyHiddenAbstractClass", ACC_ABSTRACT },
                new Object[] { "EmptyHiddenInterface", ACC_ABSTRACT|ACC_INTERFACE },
                new Object[] { "EmptyHiddenAnnotation", ACC_ANNOTATION|ACC_ABSTRACT|ACC_INTERFACE },
        };
    }

    /*
     * Test if an empty class with valid access flags can be created as a hidden class
     * as long as it does not violate the restriction of a hidden class.
     *
     * A meaningful enum type defines constants of that enum type.  So
     * enum class containing constants of its type should not be a hidden
     * class.
     */
    @Test(dataProvider = "emptyClasses")
    public void emptyHiddenClass(String name, int accessFlags) throws Exception {
        byte[] bytes = (accessFlags == ACC_ENUM) ? classBytes(name, Enum.class, accessFlags)
                                                 : classBytes(name, accessFlags);
        Class<?> hc = lookup().defineHiddenClass(bytes, false).lookupClass();
        switch (accessFlags) {
            case ACC_SYNTHETIC:
                assertTrue(hc.isSynthetic());
                assertFalse(hc.isEnum());
                assertFalse(hc.isAnnotation());
                assertFalse(hc.isInterface());
                break;
            case ACC_ENUM:
                assertFalse(hc.isSynthetic());
                assertTrue(hc.isEnum());
                assertFalse(hc.isAnnotation());
                assertFalse(hc.isInterface());
                break;
            case ACC_ABSTRACT:
                assertFalse(hc.isSynthetic());
                assertFalse(hc.isEnum());
                assertFalse(hc.isAnnotation());
                assertFalse(hc.isInterface());
                break;
            case ACC_ABSTRACT|ACC_INTERFACE:
                assertFalse(hc.isSynthetic());
                assertFalse(hc.isEnum());
                assertFalse(hc.isAnnotation());
                assertTrue(hc.isInterface());
                break;
            case ACC_ANNOTATION|ACC_ABSTRACT|ACC_INTERFACE:
                assertFalse(hc.isSynthetic());
                assertFalse(hc.isEnum());
                assertTrue(hc.isAnnotation());
                assertTrue(hc.isInterface());
                break;
            default:
                throw new IllegalArgumentException("unexpected access flag: " + accessFlags);
        }
        assertTrue(hc.isHidden());
        assertTrue(hc.getModifiers() == (ACC_PUBLIC|accessFlags));
        assertFalse(hc.isLocalClass());
        assertFalse(hc.isMemberClass());
        assertFalse(hc.isAnonymousClass());
        assertFalse(hc.isArray());
    }

    // These class files can't be defined as hidden classes
    @DataProvider(name = "cantBeHiddenClasses")
    private Object[][] cantBeHiddenClasses() {
        return new Object[][] {
                // a hidden class can't be a field's declaring type
                // enum class with static final HiddenEnum[] $VALUES:
                new Object[] { "HiddenEnum" },
                // supertype of this class is a hidden class
                new Object[] { "HiddenSuper" },
                // a record class whose equals(HiddenRecord, Object) method
                // refers to a hidden class in the parameter type and fails
                // verification.  Perhaps this method signature should be reconsidered.
                new Object[] { "HiddenRecord" },
        };
    }

    /*
     * These class files
     */
    @Test(dataProvider = "cantBeHiddenClasses", expectedExceptions = NoClassDefFoundError.class)
    public void failToDeriveAsHiddenClass(String name) throws Exception {
        byte[] bytes = Files.readAllBytes(CLASSES_DIR.resolve(name + ".class"));
        Class<?> hc = lookup().defineHiddenClass(bytes, false).lookupClass();
    }

    /*
     * A hidden class can be successfully created but fails to be reflected
     * if it refers to its own type in the descriptor.
     * e.g. Class::getMethods resolves the declaring type of fields,
     * parameter types and return type.
     */
    @Test
    public void hiddenCantReflect() throws Throwable {
        HiddenTest t = (HiddenTest)defineHiddenClass("HiddenCantReflect").newInstance();
        t.test();

        Class<?> c = t.getClass();
        Class<?>[] intfs = c.getInterfaces();
        assertTrue(intfs.length == 1);
        assertTrue(intfs[0] == HiddenTest.class);

        try {
            // this would cause loading of class HiddenCantReflect and NCDFE due
            // to error during verification
            c.getDeclaredMethods();
        } catch (NoClassDefFoundError e) {
            Throwable x = e.getCause();
            if (x == null || !(x instanceof ClassNotFoundException && x.getMessage().contains("HiddenCantReflect"))) {
                throw e;
            }
        }
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void cantDefineModule() throws Throwable {
        Path src = Paths.get("module-info.java");
        Path dir = CLASSES_DIR.resolve("m");
        Files.write(src, List.of("module m {}"), StandardCharsets.UTF_8);
        compileSources(src, dir);

        byte[] bytes = Files.readAllBytes(dir.resolve("module-info.class"));
        lookup().defineHiddenClass(bytes, false);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void cantDefineClassInAnotherPackage() throws Throwable {
        Path src = Paths.get("ClassInAnotherPackage.java");
        Files.write(src, List.of("package p;", "public class ClassInAnotherPackage {}"), StandardCharsets.UTF_8);
        compileSources(src, CLASSES_DIR);

        byte[] bytes = Files.readAllBytes(CLASSES_DIR.resolve("p").resolve("ClassInAnotherPackage.class"));
        lookup().defineHiddenClass(bytes, false);
    }

    @Test(expectedExceptions = { IllegalAccessException.class })
    public void lessPrivilegedLookup() throws Throwable {
        Lookup lookup = lookup().dropLookupMode(Lookup.PRIVATE);
        lookup.defineHiddenClass(hiddenClassBytes, false);
    }

    @Test(expectedExceptions = { UnsupportedClassVersionError.class })
    public void badClassFileVersion() throws Throwable {
        Path dir = Paths.get(System.getProperty("test.classes", "."));
        byte[] bytes = Files.readAllBytes(dir.resolve("BadClassFileVersion.class"));
        lookup().defineHiddenClass(bytes, false);
    }

    // malformed class files
    @DataProvider(name = "malformedClassFiles")
    private Object[][] malformedClassFiles() throws IOException {
        Path dir = Paths.get(System.getProperty("test.classes", "."));
        return new Object[][] {
                // `this_class` has invalid CP entry
                new Object[] { Files.readAllBytes(dir.resolve("BadClassFile.class")) },
                new Object[] { Files.readAllBytes(dir.resolve("BadClassFile2.class")) },
                // truncated file
                new Object[] { new byte[0] },
                new Object[] { new byte[] {(byte) 0xCA, (byte) 0xBA, (byte) 0xBE, (byte) 0x00} },
        };
    }

    @Test(dataProvider = "malformedClassFiles", expectedExceptions = ClassFormatError.class)
    public void badClassFile(byte[] bytes) throws Throwable {
        lookup().defineHiddenClass(bytes, false);
    }

    @DataProvider(name = "nestedTypesOrAnonymousClass")
    private Object[][] nestedTypesOrAnonymousClass() {
        return new Object[][] {
                // class file with bad InnerClasses or EnclosingMethod attribute
                new Object[] { "Outer", null },
                new Object[] { "Outer$Inner", "Outer" },
                new Object[] { "EnclosingClass", null },
                new Object[] { "EnclosingClass$1", "EnclosingClass" },
        };
    }

    @Test(dataProvider = "nestedTypesOrAnonymousClass")
    public void hasInnerClassesOrEnclosingMethodAttribute(String className, String badDeclaringClassName) throws Throwable {
        byte[] bytes = Files.readAllBytes(CLASSES_10_DIR.resolve(className + ".class"));
        Class<?> hc = lookup().defineHiddenClass(bytes, false).lookupClass();
        hiddenClassWithBadAttribute(hc, badDeclaringClassName);
    }

    // define a hidden class with static nest membership
    @Test
    public void hasStaticNestHost() throws Exception {
        byte[] bytes = Files.readAllBytes(CLASSES_DIR.resolve("Outer$Inner.class"));
        Class<?> hc = lookup().defineHiddenClass(bytes, false).lookupClass();
        hiddenClassWithBadAttribute(hc, "Outer");
    }

    @Test
    public void hasStaticNestMembers() throws Throwable {
        byte[] bytes = Files.readAllBytes(CLASSES_DIR.resolve("Outer.class"));
        Class<?> hc = lookup().defineHiddenClass(bytes, false).lookupClass();
        assertHiddenClass(hc);
        assertTrue(hc.getNestHost() == hc);
        Class<?>[] members = hc.getNestMembers();
        assertTrue(members.length == 1 && members[0] == hc);
    }

    // a hidden class with bad InnerClasses or EnclosingMethod attribute
    private void hiddenClassWithBadAttribute(Class<?> hc, String badDeclaringClassName) {
        assertTrue(hc.isHidden());
        assertTrue(hc.getCanonicalName() == null);
        assertTrue(hc.getName().contains("/"));

        if (badDeclaringClassName == null) {
            // the following reflection API assumes a good name in InnerClasses
            // or EnclosingMethod attribute can successfully be resolved.
            assertTrue(hc.getSimpleName().length() > 0);
            assertFalse(hc.isAnonymousClass());
            assertFalse(hc.isLocalClass());
            assertFalse(hc.isMemberClass());
        } else {
            declaringClassNotFound(hc, badDeclaringClassName);
        }

        // validation of nest membership
        assertTrue(hc.getNestHost() == hc);
        // validate the static nest membership
        Class<?>[] members = hc.getNestMembers();
        assertTrue(members.length == 1 && members[0] == hc);
    }

    // Class::getSimpleName, Class::isMemberClass
    private void declaringClassNotFound(Class<?> c, String cn) {
        try {
            // fail to find declaring/enclosing class
            c.isMemberClass();
            assertTrue(false);
        } catch (NoClassDefFoundError e) {
            if (!e.getMessage().equals(cn)) {
                throw e;
            }
        }
        try {
            // fail to find declaring/enclosing class
            c.getSimpleName();
            assertTrue(false);
        } catch (NoClassDefFoundError e) {
            if (!e.getMessage().equals(cn)) {
                throw e;
            }
        }
    }

    private static void singletonNest(Class<?> hc) {
        assertTrue(hc.getNestHost() == hc);
        assertTrue(hc.getNestMembers().length == 1);
        assertTrue(hc.getNestMembers()[0] == hc);
    }

    private static void assertHiddenClass(Class<?> hc) {
        assertTrue(hc.isHidden());
        assertTrue(hc.getCanonicalName() == null);
        assertTrue(hc.getName().contains("/"));
        assertFalse(hc.isAnonymousClass());
        assertFalse(hc.isLocalClass());
        assertFalse(hc.isMemberClass());
        assertFalse(hc.getSimpleName().isEmpty()); // sanity check
    }

    private static byte[] classBytes(String classname, int accessFlags) {
        return classBytes(classname, Object.class, accessFlags);
    }

    private static byte[] classBytes(String classname, Class<?> supertType, int accessFlags) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
        cw.visit(V14, ACC_PUBLIC|accessFlags, classname, null, Type.getInternalName(supertType), null);
        cw.visitEnd();

        return cw.toByteArray();
    }
}
