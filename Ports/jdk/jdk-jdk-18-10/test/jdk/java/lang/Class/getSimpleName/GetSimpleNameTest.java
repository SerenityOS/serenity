/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8057919
 * @summary Class.getSimpleName() should work for non-JLS compliant class names
 * @modules java.base/jdk.internal.org.objectweb.asm
 */
import jdk.internal.org.objectweb.asm.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class GetSimpleNameTest {
    static class NestedClass {}
    class InnerClass {}

    static Class<?> f1() {
        class LocalClass {}
        return LocalClass.class;
    }

    public static void main(String[] args) throws Exception {
        assertEquals(NestedClass.class.getSimpleName(), "NestedClass");
        assertEquals( InnerClass.class.getSimpleName(),  "InnerClass");
        assertEquals(             f1().getSimpleName(),  "LocalClass");

        java.io.Serializable anon = new java.io.Serializable() {};
        assertEquals(anon.getClass().getSimpleName(), "");

        // Java class names, prepended enclosing class name.
        testNested("p.Outer$Nested", "p.Outer", "Nested");
        testInner( "p.Outer$Inner",  "p.Inner",  "Inner");
        testLocal( "p.Outer$1Local", "p.Outer",  "Local");
        testAnon(  "p.Outer$1",      "p.Outer",       "");

        // Non-Java class names, prepended enclosing class name.
        testNested("p.$C1$Nested", "p.$C1$", "Nested");
        testInner( "p.$C1$Inner",  "p.$C1$",  "Inner");
        testLocal( "p.$C1$Local",  "p.$C1$",  "Local");
        testAnon(  "p.$C1$1",      "p.$C1$",       "");

        // Non-Java class names, unrelated class names.
        testNested("p1.$Nested$", "p2.$C1$", "Nested");
        testInner( "p1.$Inner$",  "p2.$C1$",  "Inner");
        testLocal( "p1.$Local$",  "p2.$C1$",  "Local");
        testAnon(  "p1.$anon$",   "p2.$C1$",       "");
    }

    static void testNested(String innerName, String outerName, String simpleName) throws Exception {
        BytecodeGenerator bg = new BytecodeGenerator(innerName, outerName, simpleName);
        CustomCL cl = new CustomCL(innerName, outerName, bg.getNestedClasses(true), bg.getNestedClasses(false));
        assertEquals(cl.loadClass(innerName).getSimpleName(), simpleName);
    }

    static void testInner(String innerName, String outerName, String simpleName) throws Exception {
        BytecodeGenerator bg = new BytecodeGenerator(innerName, outerName, simpleName);
        CustomCL cl = new CustomCL(innerName, outerName, bg.getInnerClasses(true), bg.getInnerClasses(false));
        assertEquals(cl.loadClass(innerName).getSimpleName(), simpleName);
    }

    static void testLocal(String innerName, String outerName, String simpleName) throws Exception {
        BytecodeGenerator bg = new BytecodeGenerator(innerName, outerName, simpleName);
        CustomCL cl = new CustomCL(innerName, outerName, bg.getLocalClasses(true), bg.getLocalClasses(false));
        assertEquals(cl.loadClass(innerName).getSimpleName(), simpleName);
    }

    static void testAnon(String innerName, String outerName, String simpleName) throws Exception {
        BytecodeGenerator bg = new BytecodeGenerator(innerName, outerName, simpleName);
        CustomCL cl = new CustomCL(innerName, outerName, bg.getAnonymousClasses(true), bg.getAnonymousClasses(false));
        assertEquals(cl.loadClass(innerName).getSimpleName(), simpleName);
    }

    static void assertEquals(Object o1, Object o2) {
        if (!java.util.Objects.equals(o1, o2)) {
            throw new AssertionError(o1 + " != " + o2);
        }
    }

    static class CustomCL extends ClassLoader {
        final String  innerName;
        final String  outerName;

        final byte[] innerClassFile;
        final byte[] outerClassFile;

        CustomCL(String innerName, String outerName, byte[] innerClassFile, byte[] outerClassFile) {
            this.innerName = innerName;
            this.outerName = outerName;
            this.innerClassFile = innerClassFile;
            this.outerClassFile = outerClassFile;
        }
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (innerName.equals(name)) {
                return defineClass(innerName, innerClassFile, 0, innerClassFile.length);
            } else if (outerName.equals(name)) {
                return defineClass(outerName, outerClassFile, 0, outerClassFile.length);
            } else {
                throw new ClassNotFoundException(name);
            }
        }
    }

    static class BytecodeGenerator {
        final String innerName;
        final String outerName;
        final String simpleName;

        BytecodeGenerator(String innerName, String outerName, String simpleName) {
            this.innerName = intl(innerName);
            this.outerName = intl(outerName);
            this.simpleName = simpleName;
        }

        static String intl(String name) { return name.replace('.', '/'); }

        static void makeDefaultCtor(ClassWriter cw) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        void makeCtxk(ClassWriter cw, boolean isInner) {
            if (isInner) {
                cw.visitOuterClass(outerName, "f", "()V");
            } else {
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "f", "()V", null, null);
                mv.visitCode();
                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }

        byte[] getNestedClasses(boolean isInner) {
            String name = (isInner ? innerName : outerName);
            ClassWriter cw = new ClassWriter(0);
            cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, name, null, "java/lang/Object", null);

            cw.visitInnerClass(innerName, outerName, simpleName, ACC_PUBLIC | ACC_STATIC);

            makeDefaultCtor(cw);
            cw.visitEnd();
            return cw.toByteArray();
        }

        byte[] getInnerClasses(boolean isInner) {
            String name = (isInner ? innerName : outerName);
            ClassWriter cw = new ClassWriter(0);
            cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, name, null, "java/lang/Object", null);

            cw.visitInnerClass(innerName, outerName, simpleName, ACC_PUBLIC);

            makeDefaultCtor(cw);
            cw.visitEnd();
            return cw.toByteArray();
        }

        byte[] getLocalClasses(boolean isInner) {
            String name = (isInner ? innerName : outerName);
            ClassWriter cw = new ClassWriter(0);
            cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, name, null, "java/lang/Object", null);

            cw.visitInnerClass(innerName, null, simpleName, ACC_PUBLIC | ACC_STATIC);
            makeCtxk(cw, isInner);

            makeDefaultCtor(cw);
            cw.visitEnd();
            return cw.toByteArray();
        }

        byte[] getAnonymousClasses(boolean isInner) {
            String name = (isInner ? innerName : outerName);
            ClassWriter cw = new ClassWriter(0);
            cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, name, null, "java/lang/Object", null);

            cw.visitInnerClass(innerName, null, null, ACC_PUBLIC | ACC_STATIC);
            makeCtxk(cw, isInner);

            makeDefaultCtor(cw);
            cw.visitEnd();
            return cw.toByteArray();
        }
    }
}
