/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary tests on constant folding of unsafe get operations
 * @library /test/lib
 *
 * @requires vm.flavor == "server" & !vm.emulatedClient
 *
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.vm.annotation
 *          java.base/jdk.internal.misc
 *
 * @library ../jsr292/patches
 * @build java.base/java.lang.invoke.MethodHandleHelper
 *
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions
 *                                 -Xbatch -XX:-TieredCompilation
 *                                 -XX:+FoldStableValues
 *                                 -XX:CompileCommand=dontinline,compiler.unsafe.UnsafeGetConstantField::checkGetAddress
 *                                 -XX:CompileCommand=dontinline,*::test*
 *                                 -XX:+UseUnalignedAccesses
 *                                 --add-reads=java.base=ALL-UNNAMED
 *                                 compiler.unsafe.UnsafeGetConstantField
 *
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions
 *                                 -Xbatch -XX:-TieredCompilation
 *                                 -XX:+FoldStableValues
 *                                 -XX:CompileCommand=dontinline,compiler.unsafe.UnsafeGetConstantField::checkGetAddress
 *                                 -XX:CompileCommand=dontinline,*::test*
 *                                 -XX:CompileCommand=inline,*Unsafe::get*
 *                                 -XX:-UseUnalignedAccesses
 *                                 --add-reads=java.base=ALL-UNNAMED
 *                                 compiler.unsafe.UnsafeGetConstantField
 */

package compiler.unsafe;

import jdk.internal.misc.Unsafe;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.vm.annotation.Stable;
import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;

import java.lang.invoke.MethodHandleHelper;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_FINAL;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_STATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACONST_NULL;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.ARETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.DUP;
import static jdk.internal.org.objectweb.asm.Opcodes.GETFIELD;
import static jdk.internal.org.objectweb.asm.Opcodes.GETSTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static jdk.internal.org.objectweb.asm.Opcodes.NEW;
import static jdk.internal.org.objectweb.asm.Opcodes.PUTFIELD;
import static jdk.internal.org.objectweb.asm.Opcodes.PUTSTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.RETURN;

public class UnsafeGetConstantField {
    static final Class<?> THIS_CLASS = UnsafeGetConstantField.class;
    static final Unsafe U = Unsafe.getUnsafe();

    public static void main(String[] args) {
        if (!Platform.isServer() || Platform.isEmulatedClient()) {
            throw new Error("TESTBUG: Not server mode");
        }
        testUnsafeGetAddress();
        testUnsafeGetField();
        testUnsafeGetFieldUnaligned();
        System.out.println("TEST PASSED");
    }

    static final long nativeAddr = U.allocateMemory(16);
    static void testUnsafeGetAddress() {
        long cookie = 0x12345678L;
        U.putAddress(nativeAddr, cookie);
        for (int i = 0; i < 20_000; i++) {
            Asserts.assertEquals(checkGetAddress(), cookie);
        }
    }

    static long checkGetAddress() {
        return U.getAddress(nativeAddr);
    }

    static void testUnsafeGetField() {
        int[] testedFlags = new int[] { 0, ACC_STATIC, ACC_FINAL, (ACC_STATIC | ACC_FINAL) };
        boolean[] boolValues = new boolean[] { false, true };
        String[] modes = new String[] { "", "Volatile" };

        for (JavaType t : JavaType.values()) {
            for (int flags : testedFlags) {
                for (boolean stable : boolValues) {
                    for (boolean hasDefaultValue : boolValues) {
                        for (String suffix : modes) {
                            runTest(t, flags, stable, hasDefaultValue, suffix);
                        }
                    }
                }
            }
        }
    }

    static void testUnsafeGetFieldUnaligned() {
        JavaType[] types = new JavaType[] { JavaType.S, JavaType.C, JavaType.I, JavaType.J };
        int[] testedFlags = new int[] { 0, ACC_STATIC, ACC_FINAL, (ACC_STATIC | ACC_FINAL) };
        boolean[] boolValues = new boolean[] { false, true };

        for (JavaType t : types) {
            for (int flags : testedFlags) {
                for (boolean stable : boolValues) {
                    for (boolean hasDefaultValue : boolValues) {
                        runTest(t, flags, stable, hasDefaultValue, "Unaligned");
                    }
                }
            }
        }
    }

    static void runTest(JavaType t, int flags, boolean stable, boolean hasDefaultValue, String postfix) {
        Generator g = new Generator(t, flags, stable, hasDefaultValue, postfix);
        Test test = g.generate();
        System.err.printf("type=%s flags=%d stable=%b default=%b post=%s\n",
                          t.typeName, flags, stable, hasDefaultValue, postfix);
        try {
            Object expected = hasDefaultValue ? t.defaultValue : t.value;
            // Trigger compilation
            for (int i = 0; i < 20_000; i++) {
                Asserts.assertEQ(expected, test.testDirect(), "i = "+ i +" direct read returns wrong value");
                Asserts.assertEQ(expected, test.testUnsafe(), "i = "+ i +" unsafe read returns wrong value");
            }

            test.changeToDefault();
            if (!hasDefaultValue && (stable || g.isFinal())) {
                Asserts.assertEQ(t.value, test.testDirect(),
                        "direct read doesn't return prev value");
                Asserts.assertEQ(test.testDirect(), test.testUnsafe());
            } else {
                Asserts.assertEQ(t.defaultValue, test.testDirect(),
                        "direct read doesn't return default value");
                Asserts.assertEQ(test.testDirect(), test.testUnsafe(),
                        "direct and unsafe reads return different values");
            }
        } catch (Throwable e) {
            try {
                g.dump();
            } catch (IOException io) {
                io.printStackTrace();
            }
            throw e;
        }
    }

    public interface Test {
        Object testDirect();
        Object testUnsafe();
        void changeToDefault();
    }

    enum JavaType {
        Z("Boolean", true, false),
        B("Byte", new Byte((byte) -1), new Byte((byte) 0)),
        S("Short", new Short((short) -1), new Short((short) 0)),
        C("Char", Character.MAX_VALUE, '\0'),
        I("Int", -1, 0),
        J("Long", -1L, 0L),
        F("Float", -1F, 0F),
        D("Double", -1D, 0D),
        L("Object", "", null);

        String typeName;
        Object value;
        Object defaultValue;
        String wrapper;
        JavaType(String name, Object value, Object defaultValue) {
            this.typeName = name;
            this.value = value;
            this.defaultValue = defaultValue;
            this.wrapper = internalName(value.getClass());
        }

        String desc() {
            if (this == JavaType.L) {
                return "Ljava/lang/Object;";
            } else {
                return name();
            }
        }
        String unsafeTypeName() {
            return typeName.equals("Object") ? "Reference" : typeName;
        }
    }

    static String internalName(Class cls) {
        return cls.getName().replace('.', '/');
    }
    static String descriptor(Class cls) {
        return String.format("L%s;", internalName(cls));
    }

    /**
     * Sample generated class:
     * static class T1 implements Test {
     *   final int f = -1;
     *   static final long FIELD_OFFSET;
     *   static final T1 t = new T1();
     *   static {
     *     FIELD_OFFSET = U.objectFieldOffset(T1.class.getDeclaredField("f"));
     *   }
     *   public Object testDirect()  { return t.f; }
     *   public Object testUnsafe()  { return U.getInt(t, FIELD_OFFSET); }
     *   public void changeToDefault() { U.putInt(t, 0, FIELD_OFFSET); }
     * }
     */
    static class Generator {
        static final String FIELD_NAME = "f";
        static final String UNSAFE_NAME = internalName(Unsafe.class);
        static final String UNSAFE_DESC = descriptor(Unsafe.class);

        final JavaType type;
        final int flags;
        final boolean stable;
        final boolean hasDefaultValue;
        final String nameSuffix;

        final String name;
        final String className;
        final String classDesc;
        final String fieldDesc;
        final byte[] classFile;

        Generator(JavaType t, int flags, boolean stable, boolean hasDefaultValue, String suffix) {
            this.type = t;
            this.flags = flags;
            this.stable = stable;
            this.hasDefaultValue = hasDefaultValue;
            this.nameSuffix = suffix;

            fieldDesc = type.desc();
            name = String.format("Test%s%s__f=%d__s=%b__d=%b",
                    type.typeName, suffix, flags, stable, hasDefaultValue);
            className = "java/lang/invoke/" + name;
            classDesc = String.format("L%s;", className);
            classFile = generateClassFile();
        }

        byte[] generateClassFile() {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className, null, "java/lang/Object",
                    new String[]{ internalName(Test.class) });

            // Declare fields
            cw.visitField(ACC_FINAL | ACC_STATIC, "t", classDesc, null, null).visitEnd();
            cw.visitField(ACC_FINAL | ACC_STATIC, "FIELD_OFFSET", "J", null, null).visitEnd();
            cw.visitField(ACC_FINAL | ACC_STATIC, "U", UNSAFE_DESC, null, null).visitEnd();
            if (isStatic()) {
                cw.visitField(ACC_FINAL | ACC_STATIC, "STATIC_BASE", "Ljava/lang/Object;", null, null).visitEnd();
            }

            FieldVisitor fv = cw.visitField(flags, FIELD_NAME, fieldDesc, null, null);
            if (stable) {
                fv.visitAnnotation(descriptor(Stable.class), true);
            }
            fv.visitEnd();

            // Methods
            {   // <init>
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                mv.visitCode();

                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                if (!isStatic()) {
                    initField(mv);
                }
                mv.visitInsn(RETURN);

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            {   // public Object testDirect() { return t.f; }
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "testDirect", "()Ljava/lang/Object;", null, null);
                mv.visitCode();

                getFieldValue(mv);
                wrapResult(mv);
                mv.visitInsn(ARETURN);

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            {   // public Object testUnsafe() { return U.getInt(t, FIELD_OFFSET); }
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "testUnsafe", "()Ljava/lang/Object;", null, null);
                mv.visitCode();

                getFieldValueUnsafe(mv);
                wrapResult(mv);
                mv.visitInsn(ARETURN);

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            {   // public void changeToDefault() { U.putInt(t, FIELD_OFFSET, 0); }
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "changeToDefault", "()V", null, null);
                mv.visitCode();
                getUnsafe(mv);
                if (isStatic()) {
                    mv.visitFieldInsn(GETSTATIC, className, "STATIC_BASE", "Ljava/lang/Object;");
                } else {
                    mv.visitFieldInsn(GETSTATIC, className, "t", classDesc);
                }
                mv.visitFieldInsn(GETSTATIC, className, "FIELD_OFFSET", "J");

                if (type.defaultValue != null) {
                    mv.visitLdcInsn(type.defaultValue);
                } else {
                    mv.visitInsn(ACONST_NULL);
                }
                String name = "put" + type.unsafeTypeName() + nameSuffix;
                mv.visitMethodInsn(INVOKEVIRTUAL, UNSAFE_NAME, name, "(Ljava/lang/Object;J" + type.desc()+ ")V", false);
                mv.visitInsn(RETURN);

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            {   // <clinit>
                MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
                mv.visitCode();

                // Cache Unsafe instance
                mv.visitMethodInsn(INVOKESTATIC, UNSAFE_NAME, "getUnsafe", "()"+UNSAFE_DESC, false);
                mv.visitFieldInsn(PUTSTATIC, className, "U", UNSAFE_DESC);

                // Create test object instance
                mv.visitTypeInsn(NEW, className);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
                mv.visitFieldInsn(PUTSTATIC, className, "t", classDesc);

                // Compute field offset
                getUnsafe(mv);
                getField(mv);
                mv.visitMethodInsn(INVOKEVIRTUAL, UNSAFE_NAME, (isStatic() ? "staticFieldOffset" : "objectFieldOffset"),
                        "(Ljava/lang/reflect/Field;)J", false);
                mv.visitFieldInsn(PUTSTATIC, className, "FIELD_OFFSET", "J");

                // Compute base offset for static field
                if (isStatic()) {
                    getUnsafe(mv);
                    getField(mv);
                    mv.visitMethodInsn(INVOKEVIRTUAL, UNSAFE_NAME, "staticFieldBase", "(Ljava/lang/reflect/Field;)Ljava/lang/Object;", false);
                    mv.visitFieldInsn(PUTSTATIC, className, "STATIC_BASE", "Ljava/lang/Object;");
                    initField(mv);
                }

                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            return cw.toByteArray();
        }

        Test generate() {
            try {
                Lookup lookup = MethodHandleHelper.IMPL_LOOKUP.in(MethodHandles.class);
                Class<?> c = lookup.defineClass(classFile);
                return (Test) c.newInstance();
            } catch(Exception e) {
                throw new Error(e);
            }
        }

        boolean isStatic() {
            return (flags & ACC_STATIC) > 0;
        }
        boolean isFinal() {
            return (flags & ACC_FINAL) > 0;
        }
        void getUnsafe(MethodVisitor mv) {
            mv.visitFieldInsn(GETSTATIC, className, "U", UNSAFE_DESC);
        }
        void getField(MethodVisitor mv) {
            mv.visitLdcInsn(Type.getType(classDesc));
            mv.visitLdcInsn(FIELD_NAME);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
        }
        void getFieldValue(MethodVisitor mv) {
            if (isStatic()) {
                mv.visitFieldInsn(GETSTATIC, className, FIELD_NAME, fieldDesc);
            } else {
                mv.visitFieldInsn(GETSTATIC, className, "t", classDesc);
                mv.visitFieldInsn(GETFIELD, className, FIELD_NAME, fieldDesc);
            }
        }
        void getFieldValueUnsafe(MethodVisitor mv) {
            getUnsafe(mv);
            if (isStatic()) {
                mv.visitFieldInsn(GETSTATIC, className, "STATIC_BASE", "Ljava/lang/Object;");
            } else {
                mv.visitFieldInsn(GETSTATIC, className, "t", classDesc);
            }
            mv.visitFieldInsn(GETSTATIC, className, "FIELD_OFFSET", "J");
            String name = "get" + type.unsafeTypeName() + nameSuffix;
            mv.visitMethodInsn(INVOKEVIRTUAL, UNSAFE_NAME, name, "(Ljava/lang/Object;J)" + type.desc(), false);
        }
        void wrapResult(MethodVisitor mv) {
            if (type != JavaType.L) {
                String desc = String.format("(%s)L%s;", type.desc(), type.wrapper);
                mv.visitMethodInsn(INVOKESTATIC, type.wrapper, "valueOf", desc, false);
            }
        }
        void initField(MethodVisitor mv) {
            if (hasDefaultValue) {
                return; // Nothing to do
            }
            if (!isStatic()) {
                mv.visitVarInsn(ALOAD, 0);
            }
            mv.visitLdcInsn(type.value);
            mv.visitFieldInsn((isStatic() ? PUTSTATIC : PUTFIELD), className, FIELD_NAME, fieldDesc);
        }

        public void dump() throws IOException {
            Path path = Paths.get(".", name + ".class").toAbsolutePath();
            System.err.println("dumping test class to " + path);
            Files.write(path, classFile);
        }
    }
}
