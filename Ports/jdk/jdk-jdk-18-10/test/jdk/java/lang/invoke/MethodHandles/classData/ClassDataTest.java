/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8230501
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @run testng/othervm ClassDataTest
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jdk.internal.org.objectweb.asm.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.lang.invoke.MethodHandles.Lookup.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static org.testng.Assert.*;

public class ClassDataTest {
    private static final Lookup LOOKUP = MethodHandles.lookup();

    @Test
    public void testOriginalAccess() throws IllegalAccessException {
        Lookup lookup = hiddenClass(20);
        assertTrue(lookup.hasFullPrivilegeAccess());

        int value = MethodHandles.classData(lookup, "_", int.class);
        assertEquals(value, 20);

        Integer i = MethodHandles.classData(lookup, "_", Integer.class);
        assertEquals(i.intValue(), 20);
    }

    /*
     * A lookup class with no class data.
     */
    @Test
    public void noClassData() throws IllegalAccessException {
        assertNull(MethodHandles.classData(LOOKUP, "_", Object.class));
    }

    @DataProvider(name = "teleportedLookup")
    private Object[][] teleportedLookup() throws ReflectiveOperationException {
        Lookup lookup = hiddenClass(30);
        Class<?> hc = lookup.lookupClass();
        assertClassData(lookup, 30);

        int fullAccess = PUBLIC|PROTECTED|PACKAGE|MODULE|PRIVATE;
        return new Object[][] {
                new Object[] { MethodHandles.privateLookupIn(hc, LOOKUP), fullAccess},
                new Object[] { LOOKUP.in(hc), fullAccess & ~(PROTECTED|PRIVATE) },
                new Object[] { lookup.dropLookupMode(PRIVATE), fullAccess & ~(PROTECTED|PRIVATE) },
        };
    }

    @Test(dataProvider = "teleportedLookup", expectedExceptions = { IllegalAccessException.class })
    public void illegalAccess(Lookup lookup, int access) throws IllegalAccessException {
        int lookupModes = lookup.lookupModes();
        assertTrue((lookupModes & ORIGINAL) == 0);
        assertEquals(lookupModes, access);
        MethodHandles.classData(lookup, "_", int.class);
    }

    @Test(expectedExceptions = { ClassCastException.class })
    public void incorrectType() throws IllegalAccessException {
        Lookup lookup = hiddenClass(20);
        MethodHandles.classData(lookup, "_", Long.class);
    }

    @Test(expectedExceptions = { IndexOutOfBoundsException.class })
    public void invalidIndex() throws IllegalAccessException {
        Lookup lookup = hiddenClass(List.of());
        MethodHandles.classDataAt(lookup, "_", Object.class, 0);
    }

    @Test(expectedExceptions = { NullPointerException.class })
    public void unboxNull() throws IllegalAccessException {
        List<Integer> list = new ArrayList<>();
        list.add(null);
        Lookup lookup = hiddenClass(list);
        MethodHandles.classDataAt(lookup, "_", int.class, 0);
    }

    @Test
    public void nullElement() throws IllegalAccessException {
        List<Object> list = new ArrayList<>();
        list.add(null);
        Lookup lookup = hiddenClass(list);
        assertTrue(MethodHandles.classDataAt(lookup, "_", Object.class, 0) == null);
    }

    @Test
    public void intClassData() throws ReflectiveOperationException {
        ClassByteBuilder builder = new ClassByteBuilder("T1-int");
        byte[] bytes = builder.classData(ACC_PUBLIC|ACC_STATIC, int.class).build();
        Lookup lookup = LOOKUP.defineHiddenClassWithClassData(bytes, 100, true);
        int value = MethodHandles.classData(lookup, "_", int.class);
        assertEquals(value, 100);
        // call through condy
        assertClassData(lookup, 100);
    }

    @Test
    public void floatClassData() throws ReflectiveOperationException {
        ClassByteBuilder builder = new ClassByteBuilder("T1-float");
        byte[] bytes = builder.classData(ACC_PUBLIC|ACC_STATIC, float.class).build();
        Lookup lookup = LOOKUP.defineHiddenClassWithClassData(bytes, 0.1234f, true);
        float value = MethodHandles.classData(lookup, "_", float.class);
        assertEquals(value, 0.1234f);
        // call through condy
        assertClassData(lookup, 0.1234f);
    }

    @Test
    public void classClassData() throws ReflectiveOperationException {
        Class<?> hc = hiddenClass(100).lookupClass();
        ClassByteBuilder builder = new ClassByteBuilder("T2");
        byte[] bytes = builder.classData(ACC_PUBLIC|ACC_STATIC, Class.class).build();
        Lookup lookup = LOOKUP.defineHiddenClassWithClassData(bytes, hc, true);
        Class<?> value = MethodHandles.classData(lookup, "_", Class.class);
        assertEquals(value, hc);
        // call through condy
        assertClassData(lookup, hc);
    }

    @Test
    public void arrayClassData() throws ReflectiveOperationException {
        ClassByteBuilder builder = new ClassByteBuilder("T3");
        byte[] bytes = builder.classData(ACC_PUBLIC|ACC_STATIC, String[].class).build();
        String[] colors = new String[] { "red", "yellow", "blue"};
        Lookup lookup = LOOKUP.defineHiddenClassWithClassData(bytes, colors, true);
        assertClassData(lookup, colors.clone());
        // class data is modifiable and not a constant
        colors[0] = "black";
        // it will get back the modified class data
        String[] value = MethodHandles.classData(lookup, "_", String[].class);
        assertEquals(value, colors);
        // even call through condy as it's not a constant
        assertClassData(lookup, colors);
    }

    @Test
    public void listClassData() throws ReflectiveOperationException {
        ClassByteBuilder builder = new ClassByteBuilder("T4");
        byte[] bytes = builder.classDataAt(ACC_PUBLIC|ACC_STATIC, Integer.class, 2).build();
        List<Integer> cd = List.of(100, 101, 102, 103);
        int expected = 102;  // element at index=2
        Lookup lookup = LOOKUP.defineHiddenClassWithClassData(bytes, cd, true);
        int value = MethodHandles.classDataAt(lookup, "_", int.class, 2);
        assertEquals(value, expected);
        // call through condy
        assertClassData(lookup, expected);
    }

    @Test
    public void arrayListClassData() throws ReflectiveOperationException {
        ClassByteBuilder builder = new ClassByteBuilder("T4");
        byte[] bytes = builder.classDataAt(ACC_PUBLIC|ACC_STATIC, Integer.class, 1).build();
        ArrayList<Integer> cd = new ArrayList<>();
        Stream.of(100, 101, 102, 103).forEach(cd::add);
        int expected = 101;  // element at index=1
        Lookup lookup = LOOKUP.defineHiddenClassWithClassData(bytes, cd, true);
        int value = MethodHandles.classDataAt(lookup, "_", int.class, 1);
        assertEquals(value, expected);
        // call through condy
        assertClassData(lookup, expected);
    }

    private static Lookup hiddenClass(int value) {
        ClassByteBuilder builder = new ClassByteBuilder("HC");
        byte[] bytes = builder.classData(ACC_PUBLIC|ACC_STATIC, int.class).build();
        try {
            return LOOKUP.defineHiddenClassWithClassData(bytes, value, true);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    private static Lookup hiddenClass(List<?> list) {
        ClassByteBuilder builder = new ClassByteBuilder("HC");
        byte[] bytes = builder.classData(ACC_PUBLIC|ACC_STATIC, List.class).build();
        try {
            return LOOKUP.defineHiddenClassWithClassData(bytes, list, true);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void condyInvokedFromVirtualMethod() throws ReflectiveOperationException {
        ClassByteBuilder builder = new ClassByteBuilder("T5");
        // generate classData instance method
        byte[] bytes = builder.classData(ACC_PUBLIC, Class.class).build();
        Lookup hcLookup = hiddenClass(100);
        assertClassData(hcLookup, 100);
        Class<?> hc = hcLookup.lookupClass();
        Lookup lookup = LOOKUP.defineHiddenClassWithClassData(bytes, hc, true);
        Class<?> value = MethodHandles.classData(lookup, "_", Class.class);
        assertEquals(value, hc);
        // call through condy
        Class<?> c = lookup.lookupClass();
        assertClassData(lookup, c.newInstance(), hc);
    }

    @Test
    public void immutableListClassData() throws ReflectiveOperationException {
        ClassByteBuilder builder = new ClassByteBuilder("T6");
        // generate classDataAt instance method
        byte[] bytes = builder.classDataAt(ACC_PUBLIC, Integer.class, 2).build();
        List<Integer> cd = List.of(100, 101, 102, 103);
        int expected = 102;  // element at index=2
        Lookup lookup = LOOKUP.defineHiddenClassWithClassData(bytes, cd, true);
        int value = MethodHandles.classDataAt(lookup, "_", int.class, 2);
        assertEquals(value, expected);
        // call through condy
        Class<?> c = lookup.lookupClass();
        assertClassData(lookup, c.newInstance() ,expected);
    }

    /*
     * The return value of MethodHandles::classDataAt is the element
     * contained in the list when the method is called.
     * If MethodHandles::classDataAt is called via condy, the value
     * will be captured as a constant.  If the class data is modified
     * after the element at the given index is computed via condy,
     * subsequent LDC of such ConstantDynamic entry will return the same
     * value. However, direct invocation of MethodHandles::classDataAt
     * will return the modified value.
     */
    @Test
    public void mutableListClassData() throws ReflectiveOperationException {
        ClassByteBuilder builder = new ClassByteBuilder("T7");
        // generate classDataAt instance method
        byte[] bytes = builder.classDataAt(ACC_PUBLIC, MethodType.class, 0).build();
        MethodType mtype = MethodType.methodType(int.class, String.class);
        List<MethodType> cd = new ArrayList<>(List.of(mtype));
        Lookup lookup = LOOKUP.defineHiddenClassWithClassData(bytes, cd, true);
        // call through condy
        Class<?> c = lookup.lookupClass();
        assertClassData(lookup, c.newInstance(), mtype);
        // modify the class data
        assertTrue(cd.remove(0) == mtype);
        cd.add(0,  MethodType.methodType(void.class));
        MethodType newMType = cd.get(0);
        // loading the element using condy returns the original value
        assertClassData(lookup, c.newInstance(), mtype);
        // direct invocation of MethodHandles.classDataAt returns the modified value
        assertEquals(MethodHandles.classDataAt(lookup, "_", MethodType.class, 0), newMType);
    }

    // helper method to extract from a class data map
    public static <T> T getClassDataEntry(Lookup lookup, String key, Class<T> type) throws IllegalAccessException {
        Map<String, T> cd = MethodHandles.classData(lookup, "_", Map.class);
        return type.cast(cd.get(key));
    }

    @Test
    public void classDataMap() throws ReflectiveOperationException {
        ClassByteBuilder builder = new ClassByteBuilder("map");
        // generate classData static method
        Handle bsm = new Handle(H_INVOKESTATIC, "ClassDataTest", "getClassDataEntry",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;",
                false);
        // generate two accessor methods to get the entries from class data
        byte[] bytes = builder.classData(ACC_PUBLIC|ACC_STATIC, Map.class)
                              .classData(ACC_PUBLIC|ACC_STATIC, "getClass",
                                         Class.class, new ConstantDynamic("class", Type.getDescriptor(Class.class), bsm))
                              .classData(ACC_PUBLIC|ACC_STATIC, "getMethod",
                                         MethodHandle.class, new ConstantDynamic("method", Type.getDescriptor(MethodHandle.class), bsm))
                              .build();

        // generate a hidden class
        Lookup hcLookup = hiddenClass(100);
        Class<?> hc = hcLookup.lookupClass();
        assertClassData(hcLookup, 100);

        MethodHandle mh = hcLookup.findStatic(hc, "classData", MethodType.methodType(int.class));
        Map<String, Object> cd = Map.of("class", hc, "method", mh);
        Lookup lookup = LOOKUP.defineHiddenClassWithClassData(bytes, cd, true);
        assertClassData(lookup, cd);

        // validate the entries from the class data map
        Class<?> c = lookup.lookupClass();
        Method m = c.getMethod("getClass");
        Class<?> v = (Class<?>)m.invoke(null);
        assertEquals(hc, v);

        Method m1 = c.getMethod("getMethod");
        MethodHandle v1 = (MethodHandle) m1.invoke(null);
        assertEquals(mh, v1);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void nonDefaultName() throws ReflectiveOperationException {
        ClassByteBuilder builder = new ClassByteBuilder("nonDefaultName");
        byte[] bytes = builder.classData(ACC_PUBLIC|ACC_STATIC, Class.class)
                              .build();
        Lookup lookup = LOOKUP.defineHiddenClassWithClassData(bytes, ClassDataTest.class, true);
        assertClassData(lookup, ClassDataTest.class);
        // throw IAE
        MethodHandles.classData(lookup, "non_default_name", Class.class);
    }

    static class ClassByteBuilder {
        private static final String OBJECT_CLS = "java/lang/Object";
        private static final String MHS_CLS = "java/lang/invoke/MethodHandles";
        private static final String CLASS_DATA_BSM_DESCR =
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;";
        private final ClassWriter cw;
        private final String classname;

        /**
         * A builder to generate a class file to access class data
         * @param classname
         */
        ClassByteBuilder(String classname) {
            this.classname = classname;
            this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cw.visit(V14, ACC_FINAL, classname, null, OBJECT_CLS, null);
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, OBJECT_CLS, "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        byte[] build() {
            cw.visitEnd();
            byte[] bytes = cw.toByteArray();
            Path p = Paths.get(classname + ".class");
                try (OutputStream os = Files.newOutputStream(p)) {
                os.write(bytes);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return bytes;
        }

        /*
         * Generate classData method to load class data via condy
         */
        ClassByteBuilder classData(int accessFlags, Class<?> returnType) {
            MethodType mtype = MethodType.methodType(returnType);
            MethodVisitor mv = cw.visitMethod(accessFlags,
                                             "classData",
                                              mtype.descriptorString(), null, null);
            mv.visitCode();
            Handle bsm = new Handle(H_INVOKESTATIC, MHS_CLS, "classData",
                                    CLASS_DATA_BSM_DESCR,
                                    false);
            ConstantDynamic dynamic = new ConstantDynamic("_", Type.getDescriptor(returnType), bsm);
            mv.visitLdcInsn(dynamic);
            mv.visitInsn(returnType == int.class ? IRETURN :
                            (returnType == float.class ? FRETURN : ARETURN));
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return this;
        }

        /*
         * Generate classDataAt method to load an element from class data via condy
         */
        ClassByteBuilder classDataAt(int accessFlags, Class<?> returnType, int index) {
            MethodType mtype = MethodType.methodType(returnType);
            MethodVisitor mv = cw.visitMethod(accessFlags,
                                              "classData",
                                               mtype.descriptorString(), null, null);
            mv.visitCode();
            Handle bsm = new Handle(H_INVOKESTATIC, "java/lang/invoke/MethodHandles", "classDataAt",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;I)Ljava/lang/Object;",
                        false);
            ConstantDynamic dynamic = new ConstantDynamic("_", Type.getDescriptor(returnType), bsm, index);
            mv.visitLdcInsn(dynamic);
            mv.visitInsn(returnType == int.class? IRETURN : ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return this;
        }

        ClassByteBuilder classData(int accessFlags, String name, Class<?> returnType, ConstantDynamic dynamic) {
            MethodType mtype = MethodType.methodType(returnType);
            MethodVisitor mv = cw.visitMethod(accessFlags,
                                              name,
                                              mtype.descriptorString(), null, null);
            mv.visitCode();
            mv.visitLdcInsn(dynamic);
            mv.visitInsn(returnType == int.class? IRETURN : ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return this;
        }
    }

    /*
     * Load an int constant from class data via condy and
     * verify it matches the given value.
     */
    private void assertClassData(Lookup lookup, int value) throws ReflectiveOperationException {
        Class<?> c = lookup.lookupClass();
        Method m = c.getMethod("classData");
        int v = (int) m.invoke(null);
        assertEquals(value, v);
    }

    /*
     * Load an int constant from class data via condy and
     * verify it matches the given value.
     */
    private void assertClassData(Lookup lookup, Object o, int value) throws ReflectiveOperationException {
        Class<?> c = lookup.lookupClass();
        Method m = c.getMethod("classData");
        int v = (int) m.invoke(o);
        assertEquals(value, v);
    }

    /*
     * Load a float constant from class data via condy and
     * verify it matches the given value.
     */
    private void assertClassData(Lookup lookup, float value) throws ReflectiveOperationException {
        Class<?> c = lookup.lookupClass();
        Method m = c.getMethod("classData");
        float v = (float) m.invoke(null);
        assertEquals(value, v);
    }

    /*
     * Load a Class constant from class data via condy and
     * verify it matches the given value.
     */
    private void assertClassData(Lookup lookup, Class<?> value) throws ReflectiveOperationException {
        Class<?> c = lookup.lookupClass();
        Method m = c.getMethod("classData");
        Class<?> v = (Class<?>)m.invoke(null);
        assertEquals(value, v);
    }

    /*
     * Load a Class from class data via condy and
     * verify it matches the given value.
     */
    private void assertClassData(Lookup lookup, Object o, Class<?> value) throws ReflectiveOperationException {
        Class<?> c = lookup.lookupClass();
        Method m = c.getMethod("classData");
        Object v = m.invoke(o);
        assertEquals(value, v);
    }

    /*
     * Load an Object from class data via condy and
     * verify it matches the given value.
     */
    private void assertClassData(Lookup lookup, Object value) throws ReflectiveOperationException {
        Class<?> c = lookup.lookupClass();
        Method m = c.getMethod("classData");
        Object v = m.invoke(null);
        assertEquals(value, v);
    }

    /*
     * Load an Object from class data via condy and
     * verify it matches the given value.
     */
    private void assertClassData(Lookup lookup, Object o, Object value) throws ReflectiveOperationException {
        Class<?> c = lookup.lookupClass();
        Method m = c.getMethod("classData");
        Object v = m.invoke(o);
        assertEquals(value, v);
    }
}


