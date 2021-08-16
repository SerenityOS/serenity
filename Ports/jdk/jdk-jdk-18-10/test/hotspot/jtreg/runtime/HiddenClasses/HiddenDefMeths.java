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
 * @summary Tests a hidden class that implements interfaces with default methods.
 * @library /testlibrary
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.management
 * @run main HiddenDefMeths
 */

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Type;

import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_SUPER;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.ARETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.DUP;
import static jdk.internal.org.objectweb.asm.Opcodes.GETFIELD;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.PUTFIELD;
import static jdk.internal.org.objectweb.asm.Opcodes.RETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.V1_8;

public class HiddenDefMeths {

    interface Resource {
        Pointer ptr();
    }

    interface Struct extends Resource {
       StructPointer ptr();
    }

    interface Pointer { }

    interface StructPointer extends Pointer { }

    interface I extends Struct {
        void m();
    }

    static String IMPL_PREFIX = "$$impl";
    static String PTR_FIELD_NAME = "ptr";

    // Generate a class similar to:
    //
    // public class HiddenDefMeths$I$$impl implements HiddenDefMeths$I, HiddenDefMeths$Struct {
    //
    //     public HiddenDefMeths$StructPointer ptr;
    //
    //     public HiddenDefMeths$I$$impl(HiddenDefMeths$StructPointer p) {
    //         ptr = p;
    //     }
    //
    //     public HiddenDefMeths$StructPointer ptr() {
    //         return ptr;
    //     }
    // }
    //
    byte[] generate(Class<?> iface) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        String ifaceTypeName = Type.getInternalName(iface);
        String proxyClassName = ifaceTypeName + IMPL_PREFIX;
        // class definition
        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, proxyClassName,
                desc(Object.class) + desc(ifaceTypeName) + desc(Struct.class),
                name(Object.class),
                new String[] { ifaceTypeName, name(Struct.class) });

        cw.visitField(ACC_PUBLIC, PTR_FIELD_NAME, desc(StructPointer.class), desc(StructPointer.class), null);
        cw.visitEnd();

        // constructor
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>",
                meth(desc(void.class), desc(StructPointer.class)),
                meth(desc(void.class), desc(StructPointer.class)), null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, name(Object.class), "<init>", meth(desc(void.class)), false);
        mv.visitVarInsn(ALOAD, 1);
        // Execution of this PUTFIELD instruction causes the bug's ClassNotFoundException.
        mv.visitFieldInsn(PUTFIELD, proxyClassName, PTR_FIELD_NAME, desc(StructPointer.class));
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // ptr() impl
        mv = cw.visitMethod(ACC_PUBLIC, PTR_FIELD_NAME, meth(desc(StructPointer.class)),
                meth(desc(StructPointer.class)), null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, proxyClassName, PTR_FIELD_NAME, desc(StructPointer.class));
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        return cw.toByteArray();
    }

    String name(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            throw new IllegalStateException();
        } else if (clazz.isArray()) {
            return desc(clazz);
        } else {
            return clazz.getName().replaceAll("\\.", "/");
        }
    }

    String desc(Class<?> clazz) {
        String mdesc = MethodType.methodType(clazz).toMethodDescriptorString();
        return mdesc.substring(mdesc.indexOf(')') + 1);
    }

    String desc(String clazzName) {
        return "L" + clazzName + ";";
    }

    String gen(String clazz, String... typeargs) {
        return clazz.substring(0, clazz.length() - 1) + Stream.of(typeargs).collect(Collectors.joining("", "<", ">")) + ";";
    }

    String meth(String restype, String... argtypes) {
        return Stream.of(argtypes).collect(Collectors.joining("", "(", ")")) + restype;
    }

    String meth(Method m) {
        return MethodType.methodType(m.getReturnType(), m.getParameterTypes()).toMethodDescriptorString();
    }

    public static void main(String[] args) throws Throwable {
        byte[] bytes = new HiddenDefMeths().generate(I.class);
        Lookup lookup = MethodHandles.lookup();
        Class<?> cl = lookup.defineHiddenClass(bytes, false, NESTMATE).lookupClass();
        I i = (I)cl.getConstructors()[0].newInstance(new Object[] { null });
    }
}
