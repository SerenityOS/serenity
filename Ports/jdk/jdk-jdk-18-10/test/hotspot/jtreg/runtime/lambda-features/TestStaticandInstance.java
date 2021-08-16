/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8087342
 * @summary Test linkresolver search static, instance and overpass duplicates
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:-BytecodeVerificationRemote -XX:-BytecodeVerificationLocal TestStaticandInstance
 */


import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class TestStaticandInstance {
  static final String stringC = "C";
  static final String stringD = "D";
  static final String stringI = "I";

  public static void main(String args[]) throws Throwable {
    ClassLoader cl = new ClassLoader() {
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class retClass;
        if ((retClass = findLoadedClass(name)) != null) {
           return retClass;
        }
        if (stringC.equals(name)) {
            byte[] classFile=dumpC();
            return defineClass(stringC, classFile, 0, classFile.length);
        }
        if (stringD.equals(name)) {
            byte[] classFile=dumpD();
            return defineClass(stringD, classFile, 0, classFile.length);
        }
        if (stringI.equals(name)) {
            byte[] classFile=dumpI();
            return defineClass(stringI, classFile, 0, classFile.length);
        }
        return super.loadClass(name);
      }
    };

    Class classC = cl.loadClass(stringC);
    Class classI = cl.loadClass(stringI);

    try {
      int staticret = (Integer)cl.loadClass(stringD).getDeclaredMethod("CallStatic").invoke(null);
      if (staticret != 1) {
        throw new RuntimeException("invokestatic failed to call correct method");
      }
      System.out.println("staticret: " + staticret); // should be 1

      int invokeinterfaceret = (Integer)cl.loadClass(stringD).getDeclaredMethod("CallInterface").invoke(null);
      if (invokeinterfaceret != 0) {
        throw new RuntimeException(String.format("Expected java.lang.AbstractMethodError, got %d", invokeinterfaceret));
      }
      System.out.println("invokeinterfaceret: AbstractMethodError");

      int invokevirtualret = (Integer)cl.loadClass(stringD).getDeclaredMethod("CallVirtual").invoke(null);
      if (invokevirtualret != 0) {
        throw new RuntimeException(String.format("Expected java.lang.IncompatibleClassChangeError, got %d", invokevirtualret));
      }
      System.out.println("invokevirtualret: IncompatibleClassChangeError");
    } catch (java.lang.Throwable e) {
      throw new RuntimeException("Unexpected exception: " + e.getMessage());
    }
  }

/*
interface I {
  public int m(); // abstract
  default int q() { return 3; } // trigger defmeth processing: C gets AME overpass
}

// C gets static, private and AME overpass m()I with the verifier turned off
class C implements I {
  static int m() { return 1;}  // javac with "n()" and patch to "m()"
  private int m() { return 2;} // javac with public and patch to private
}

public class D {
  public static int CallStatic() {
    int staticret = C.m();    // javac with "C.n" and patch to "C.m"
    return staticret;
  }
  public static int CallInterface() throws AbstractMethodError{
    try {
      I myI = new C();
      return myI.m();
    } catch (java.lang.AbstractMethodError e) {
      return 0; // for success
    }
  }
  public static int CallVirtual() {
    try {
      C myC = new C();
      return myC.m();
    } catch (java.lang.IncompatibleClassChangeError e) {
      return 0; // for success
    }
  }
}
*/

  public static byte[] dumpC() {

    ClassWriter cw = new ClassWriter(0);
    FieldVisitor fv;
    MethodVisitor mv;
    AnnotationVisitor av0;

    cw.visit(52, ACC_SUPER, "C", null, "java/lang/Object", new String[] { "I" });

    {
      mv = cw.visitMethod(0, "<init>", "()V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_STATIC, "m", "()I", null, null);
      mv.visitCode();
      mv.visitInsn(ICONST_1);
      mv.visitInsn(IRETURN);
      mv.visitMaxs(1, 0);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PRIVATE, "m", "()I", null, null);
      mv.visitCode();
      mv.visitInsn(ICONST_2);
      mv.visitInsn(IRETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }

  public static byte[] dumpD () {

    ClassWriter cw = new ClassWriter(0);
    FieldVisitor fv;
    MethodVisitor mv;
    AnnotationVisitor av0;

    cw.visit(52, ACC_PUBLIC + ACC_SUPER, "D", null, "java/lang/Object", null);

    {
      mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "CallStatic", "()I", null, null);
      mv.visitCode();
      mv.visitMethodInsn(INVOKESTATIC, "C", "m", "()I", false);
      mv.visitVarInsn(ISTORE, 0);
      mv.visitVarInsn(ILOAD, 0);
      mv.visitInsn(IRETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "CallInterface", "()I", null, new String[] { "java/lang/AbstractMethodError" });
      mv.visitCode();
      Label l0 = new Label();
      Label l1 = new Label();
      Label l2 = new Label();
      mv.visitTryCatchBlock(l0, l1, l2, "java/lang/AbstractMethodError");
      mv.visitLabel(l0);
      mv.visitTypeInsn(NEW, "C");
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, "C", "<init>", "()V", false);
      mv.visitVarInsn(ASTORE, 0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEINTERFACE, "I", "m", "()I", true);
      mv.visitLabel(l1);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l2);
      mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/AbstractMethodError"});
      mv.visitVarInsn(ASTORE, 0);
      mv.visitInsn(ICONST_0);
      mv.visitInsn(IRETURN);
      mv.visitMaxs(2, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "CallVirtual", "()I", null, null);
      mv.visitCode();
      Label l0 = new Label();
      Label l1 = new Label();
      Label l2 = new Label();
      mv.visitTryCatchBlock(l0, l1, l2, "java/lang/IncompatibleClassChangeError");
      mv.visitLabel(l0);
      mv.visitTypeInsn(NEW, "C");
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, "C", "<init>", "()V", false);
      mv.visitVarInsn(ASTORE, 0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "C", "m", "()I", false);
      mv.visitLabel(l1);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l2);
      mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/IncompatibleClassChangeError"});
      mv.visitVarInsn(ASTORE, 0);
      mv.visitInsn(ICONST_0);
      mv.visitInsn(IRETURN);
      mv.visitMaxs(2, 1);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }

  public static byte[] dumpI() {

    ClassWriter cw = new ClassWriter(0);
    FieldVisitor fv;
    MethodVisitor mv;
    AnnotationVisitor av0;

    cw.visit(52, ACC_ABSTRACT + ACC_INTERFACE, "I", null, "java/lang/Object", null);

    {
      mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "m", "()I", null, null);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "q", "()I", null, null);
      mv.visitCode();
      mv.visitInsn(ICONST_3);
      mv.visitInsn(IRETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }
}
