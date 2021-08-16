/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8051045 8166974
 * @summary Test exceptions from invokedynamic and the bootstrap method
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @run main BootstrapMethodErrorTest
 */

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class BootstrapMethodErrorTest {

    static abstract class IndyClassloader extends ClassLoader implements Opcodes {

        public IndyClassloader() {
            super(BootstrapMethodErrorTest.class.getClassLoader());
        }

        @Override
        public Class findClass(String name) throws ClassNotFoundException {
            byte[] b;
            try {
                b = loadClassData(name);
            }
            catch (Throwable th) {
                throw new ClassNotFoundException("Loading error", th);
            }
            return defineClass(name, b, 0, b.length);
        }

        static final String BOOTSTRAP_METHOD_CLASS_NAME = "C";

        static final String BOOTSTRAP_METHOD_NAME = "bsm";

        static final String INDY_CALLER_CLASS_NAME = "Exec";

        static final String BOOTSTRAP_METHOD_DESC = MethodType.methodType(
                Object.class, MethodHandles.Lookup.class, String.class, MethodType.class).
                toMethodDescriptorString();

        private byte[] loadClassData(String name) throws Exception {
            ClassWriter cw = new ClassWriter(
                    ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            if (name.equals(BOOTSTRAP_METHOD_CLASS_NAME)) {
                defineIndyBootstrapMethodClass(cw);
                return cw.toByteArray();
            }
            else if (name.equals("Exec")) {
                defineIndyCallingClass(cw);
                return cw.toByteArray();
            }
            return null;
        }

        void defineIndyCallingClass(ClassWriter cw) {
            cw.visit(52, ACC_SUPER | ACC_PUBLIC, INDY_CALLER_CLASS_NAME, null, "java/lang/Object", null);
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "invoke", "()V", null, null);
            mv.visitCode();
            Handle h = new Handle(H_INVOKESTATIC,
                                  BOOTSTRAP_METHOD_CLASS_NAME, BOOTSTRAP_METHOD_NAME,
                                  BOOTSTRAP_METHOD_DESC, false);
            mv.visitInvokeDynamicInsn(BOOTSTRAP_METHOD_CLASS_NAME, "()V", h);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            cw.visitEnd();
        }

        void defineIndyBootstrapMethodClass(ClassWriter cw) {
            cw.visit(52, ACC_SUPER | ACC_PUBLIC,
                     BOOTSTRAP_METHOD_CLASS_NAME, null, "java/lang/Object", null);
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC,
                                              BOOTSTRAP_METHOD_NAME, BOOTSTRAP_METHOD_DESC, null, null);
            mv.visitCode();
            defineIndyBootstrapMethodBody(mv);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        void defineIndyBootstrapMethodBody(MethodVisitor mv) {
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        }

        void invoke() throws Exception {
            Class.forName(BOOTSTRAP_METHOD_CLASS_NAME, true, this);
            Class<?> exec = Class.forName(INDY_CALLER_CLASS_NAME, true, this);
            exec.getMethod("invoke").invoke(null);
        }

        void test() throws Exception {
            Class.forName(BOOTSTRAP_METHOD_CLASS_NAME, true, this);
            Class<?> exec = Class.forName(INDY_CALLER_CLASS_NAME, true, this);
            try {
                exec.getMethod("invoke").invoke(null);
                throw new RuntimeException("Expected InvocationTargetException but no exception at all was thrown");
            } catch (InvocationTargetException e) {
                Throwable t = e.getCause();
                for (Class<? extends Throwable> etc : expectedThrowableClasses()) {
                    if (!etc.isInstance(t)) {
                        throw new RuntimeException(
                                "Expected " + etc.getName() + " but got another exception: "
                                + t.getClass().getName(),
                                t);
                    }
                    t = t.getCause();
                }
            }
        }

        abstract List<Class<? extends Throwable>> expectedThrowableClasses();
    }

    // Methods called by a bootstrap method

    public static CallSite getCallSite() {
        try {
            MethodHandle mh = MethodHandles.lookup().findStatic(
                    BootstrapMethodErrorTest.class,
                    "target",
                    MethodType.methodType(Object.class, Object.class));
            return new ConstantCallSite(mh);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static Object target(Object o) {
        return null;
    }

    static class TestThrowable extends Throwable {}
    public static void throwsTestThrowable() throws Throwable {
        throw new TestThrowable();
    }

    static class TestError extends Error {}
    public static void throwsTestError() {
        throw new TestError();
    }

    static class TestRuntimeException extends RuntimeException {}
    public static void throwsTestRuntimeException() {
        throw new TestRuntimeException();
    }

    static class TestCheckedException extends Exception {}
    public static void throwsTestCheckedException() throws TestCheckedException {
        throw new TestCheckedException();
    }


    // Test classes

    static class InaccessibleBootstrapMethod extends IndyClassloader {

        void defineIndyBootstrapMethodClass(ClassWriter cw) {
            cw.visit(52, ACC_SUPER | ACC_PUBLIC,
                     BOOTSTRAP_METHOD_CLASS_NAME, null, "java/lang/Object", null);
            // Bootstrap method is declared to be private
            MethodVisitor mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC,
                                              BOOTSTRAP_METHOD_NAME, BOOTSTRAP_METHOD_DESC, null, null);
            mv.visitCode();
            defineIndyBootstrapMethodBody(mv);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        @Override
        List<Class<? extends Throwable>> expectedThrowableClasses() {
            return List.of(IllegalAccessError.class);
        }
    }

    static class BootstrapMethodDoesNotReturnCallSite extends IndyClassloader {

        void defineIndyBootstrapMethodBody(MethodVisitor mv) {
            // return null from the bootstrap method,
            // which cannot be cast to CallSite
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        }

        @Override
        List<Class<? extends Throwable>> expectedThrowableClasses() {
            return List.of(BootstrapMethodError.class, ClassCastException.class);
        }
    }

    static class BootstrapMethodCallSiteHasWrongTarget extends IndyClassloader {

        @Override
        void defineIndyBootstrapMethodBody(MethodVisitor mv) {
            // Invoke the method BootstrapMethodErrorTest.getCallSite to obtain
            // a CallSite instance whose target is different from that of
            // the indy call site
            mv.visitMethodInsn(INVOKESTATIC, "BootstrapMethodErrorTest",
                               "getCallSite", "()Ljava/lang/invoke/CallSite;", false);
            mv.visitInsn(ARETURN);
        }

        @Override
        List<Class<? extends Throwable>> expectedThrowableClasses() {
            return List.of(BootstrapMethodError.class, WrongMethodTypeException.class);
        }
    }

    abstract static class BootstrapMethodThrows extends IndyClassloader {
        final String methodName;

        public BootstrapMethodThrows(Class<? extends Throwable> t) {
            this.methodName = "throws" + t.getSimpleName();
        }

        @Override
        void defineIndyBootstrapMethodBody(MethodVisitor mv) {
            // Invoke the method whose name is methodName which will throw
            // an exception
            mv.visitMethodInsn(INVOKESTATIC, "BootstrapMethodErrorTest",
                               methodName, "()V", false);
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        }
    }

    static class BootstrapMethodThrowsThrowable extends BootstrapMethodThrows {

        public BootstrapMethodThrowsThrowable() {
            super(TestThrowable.class);
        }

        @Override
        List<Class<? extends Throwable>> expectedThrowableClasses() {
            return List.of(BootstrapMethodError.class, TestThrowable.class);
        }
    }

    static class BootstrapMethodThrowsError extends BootstrapMethodThrows {

        public BootstrapMethodThrowsError() {
            super(TestError.class);
        }

        @Override
        List<Class<? extends Throwable>> expectedThrowableClasses() {
            return List.of(TestError.class);
        }
    }

    static class BootstrapMethodThrowsRuntimeException extends BootstrapMethodThrows {

        public BootstrapMethodThrowsRuntimeException() {
            super(TestRuntimeException.class);
        }

        @Override
        List<Class<? extends Throwable>> expectedThrowableClasses() {
            return List.of(BootstrapMethodError.class, TestRuntimeException.class);
        }
    }

    static class BootstrapMethodThrowsCheckedException extends BootstrapMethodThrows {

        public BootstrapMethodThrowsCheckedException() {
            super(TestCheckedException.class);
        }

        @Override
        List<Class<? extends Throwable>> expectedThrowableClasses() {
            return List.of(BootstrapMethodError.class, TestCheckedException.class);
        }
    }


    public static void main(String[] args) throws Exception {
        new InaccessibleBootstrapMethod().test();
        new BootstrapMethodDoesNotReturnCallSite().test();
        new BootstrapMethodCallSiteHasWrongTarget().test();
        new BootstrapMethodThrowsThrowable().test();
        new BootstrapMethodThrowsError().test();
        new BootstrapMethodThrowsRuntimeException().test();
        new BootstrapMethodThrowsCheckedException().test();
    }
}
