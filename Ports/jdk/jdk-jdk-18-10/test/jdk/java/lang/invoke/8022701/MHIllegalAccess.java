/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8022701
 * @summary Illegal access exceptions via methodhandle invocations threw wrong error.
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @compile -XDignore.symbol.file BogoLoader.java InvokeSeveralWays.java MHIllegalAccess.java MethodSupplier.java
 * @run main/othervm MHIllegalAccess
 */

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

public class MHIllegalAccess implements Opcodes {

   public static void main(String args[]) throws Throwable  {
      System.out.println("Classpath is " + System.getProperty("java.class.path"));
      System.out.println();

      /**
       * Make method m be private to provoke an IllegalAccessError.
       */
      BogoLoader.VisitorMaker privatize = new BogoLoader.VisitorMaker() {
           public ClassVisitor make(ClassVisitor cv) {
               return new ClassVisitor(Opcodes.ASM5, cv) {
                   public MethodVisitor visitMethod(int access, String name, String desc,
                           String signature, String[] exceptions) {
                       if (name.equals("m"))
                           access = (access | ACC_PRIVATE) & ~ (ACC_PUBLIC | ACC_PROTECTED);
                           return super.visitMethod(access, name, desc, signature, exceptions);
                   }
               };
           }
       };

     /**
       * Rename method m as nemo to provoke a NoSuchMethodError.
       */
     BogoLoader.VisitorMaker changeName = new BogoLoader.VisitorMaker() {
           public ClassVisitor make(ClassVisitor cv) {
               return new ClassVisitor(Opcodes.ASM5, cv) {
                   public MethodVisitor visitMethod(int access, String name, String desc,
                           String signature, String[] exceptions) {
                       if (name.equals("m"))
                           name = "nemo";
                           return super.visitMethod(access, name, desc, signature, exceptions);
                   }
               };
           }
       };

      int failures = 0;
      failures += testOneError(privatize, args, IllegalAccessError.class);
      failures += testOneError(changeName, args, NoSuchMethodError.class);
      if (failures > 0) {
          System.out.println("Saw " + failures + " failures, see standard out for details");
          throw new Error("FAIL test");
      }
   }

   /**
    *
    * @param vm VisitorMaker, to be stored in a table and passed to a BogoLoader
    * @param args A copy of the main args, to be passed on to InvokeSeveralWays.test
    * @param expected The class of the exception that should be thrown after
    *                 attempted invocation of MethodSupplier.m.
    * @throws ClassNotFoundException
    * @throws Throwable
    */
    private static int testOneError(BogoLoader.VisitorMaker vm, String[] args, Class expected) throws ClassNotFoundException, Throwable {
      HashMap<String, BogoLoader.VisitorMaker> replace = new HashMap<String, BogoLoader.VisitorMaker>();
      replace.put("MethodSupplier", vm);

      HashSet<String> in_bogus = new HashSet<String>();
        in_bogus.add("InvokeSeveralWays");
        in_bogus.add("MyFunctionalInterface");
        in_bogus.add("Invoker");

        BogoLoader bl = new BogoLoader(in_bogus, replace);
        Class<?> isw = bl.loadClass("InvokeSeveralWays");
        Object[] arg_for_args = new Object[2];
        arg_for_args[0] = args;
        arg_for_args[1] = expected;
        try {
            Object result = isw.getMethod("test", String[].class, Class.class).invoke(null, arg_for_args);
            return (Integer)result;
        } catch (InvocationTargetException e) {
            Throwable th = e.getCause();
            throw th == null ? e : th;
        }
    }
}
