/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package compiler.cha;

import jdk.internal.misc.Unsafe;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.vm.annotation.DontInline;
import sun.hotspot.WhiteBox;
import sun.hotspot.code.NMethod;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.Callable;

import static jdk.internal.org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static jdk.internal.org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static jdk.test.lib.Asserts.assertTrue;

public class Utils {
    public static final Unsafe U = Unsafe.getUnsafe();

    interface Test<T> {
        void call(T o);
        T receiver(int id);

        default Runnable monomophic() {
            return () -> {
                call(receiver(0)); // 100%
            };
        }

        default Runnable bimorphic() {
            return () -> {
                call(receiver(0)); // 50%
                call(receiver(1)); // 50%
            };
        }

        default Runnable polymorphic() {
            return () -> {
                for (int i = 0; i < 23; i++) {
                    call(receiver(0)); // 92%
                }
                call(receiver(1)); // 4%
                call(receiver(2)); // 4%
            };
        }

        default Runnable megamorphic() {
            return () -> {
                call(receiver(0)); // 33%
                call(receiver(1)); // 33%
                call(receiver(2)); // 33%
            };
        }

        default void load(Class<?>... cs) {
            // nothing to do
        }

        default void initialize(Class<?>... cs) {
            for (Class<?> c : cs) {
                U.ensureClassInitialized(c);
            }
        }

        default void repeat(int cnt, Runnable r) {
            for (int i = 0; i < cnt; i++) {
                r.run();
            }
        }
    }

    public static abstract class ATest<T> implements Test<T> {
        public static final WhiteBox WB = WhiteBox.getWhiteBox();

        public static final Object CORRECT = new Object();
        public static final Object WRONG   = new Object();

        final Method TEST;
        private final Class<T> declared;
        private final Class<?> receiver;

        private final HashMap<Integer, T> receivers = new HashMap<>();

        public ATest(Class<T> declared, Class<?> receiver) {
            this.declared = declared;
            this.receiver = receiver;
            TEST = compute(() -> this.getClass().getDeclaredMethod("test", declared));
        }

        @DontInline
        public abstract Object test(T i);

        public abstract void checkInvalidReceiver();

        public T receiver(int id) {
            return receivers.computeIfAbsent(id, (i -> {
                try {
                    MyClassLoader cl = (MyClassLoader) receiver.getClassLoader();
                    Class<?> sub = cl.subclass(receiver, i);
                    return (T)sub.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new Error(e);
                }
            }));
        }


        public void compile(Runnable r) {
            while (!WB.isMethodCompiled(TEST)) {
                for (int i = 0; i < 100; i++) {
                    r.run();
                }
            }
            assertCompiled(); // record nmethod info
        }

        private NMethod prevNM = null;

        public void assertNotCompiled() {
            NMethod curNM = NMethod.get(TEST, false);
            assertTrue(prevNM != null); // was previously compiled
            assertTrue(curNM == null || prevNM.compile_id != curNM.compile_id); // either no nmethod present or recompiled
            prevNM = curNM; // update nmethod info
        }

        public void assertCompiled() {
            NMethod curNM = NMethod.get(TEST, false);
            assertTrue(curNM != null); // nmethod is present
            assertTrue(prevNM == null || prevNM.compile_id == curNM.compile_id); // no recompilations if nmethod present
            prevNM = curNM; // update nmethod info
        }

        @Override
        public void call(T i) {
            assertTrue(test(i) != WRONG);
        }
    }

    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface TestCase {}

    static void run(Class<?> test) {
        try {
            for (Method m : test.getDeclaredMethods()) {
                if (m.isAnnotationPresent(TestCase.class)) {
                    System.out.println(m.toString());
                    ClassLoader cl = new MyClassLoader(test);
                    Class<?> c = cl.loadClass(test.getName());
                    c.getMethod(m.getName()).invoke(c.getDeclaredConstructor().newInstance());
                }
            }
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static class ObjectToStringHelper {
        static Object testHelper(Object o) {
            throw new Error("not used");
        }
    }
    static class ObjectHashCodeHelper {
        static int testHelper(Object o) {
            throw new Error("not used");
        }
    }

    static final class MyClassLoader extends ClassLoader {
        private final Class<?> test;

        MyClassLoader(Class<?> test) {
            this.test = test;
        }

        static String intl(String s) {
            return s.replace('.', '/');
        }

        Class<?> subclass(Class<?> c, int id) {
            String name = c.getName() + id;
            Class<?> sub = findLoadedClass(name);
            if (sub == null) {
                ClassWriter cw = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
                cw.visit(52, ACC_PUBLIC | ACC_SUPER, intl(name), null, intl(c.getName()), null);

                { // Default constructor: <init>()V
                    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                    mv.visitCode();
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKESPECIAL, intl(c.getName()), "<init>", "()V", false);
                    mv.visitInsn(RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }

                byte[] classFile = cw.toByteArray();
                return defineClass(name, classFile, 0, classFile.length);
            }
            return sub;
        }

        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException
        {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    c = getParent().loadClass(name);
                    if (name.endsWith("ObjectToStringHelper")) {
                        ClassWriter cw = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
                        cw.visit(52, ACC_PUBLIC | ACC_SUPER, intl(name), null, "java/lang/Object", null);

                        {
                            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "testHelper", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
                            mv.visitCode();
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitMethodInsn(INVOKEINTERFACE, intl(test.getName()) + "$I", "toString", "()Ljava/lang/String;", true);
                            mv.visitInsn(ARETURN);
                            mv.visitMaxs(0, 0);
                            mv.visitEnd();
                        }

                        byte[] classFile = cw.toByteArray();
                        return defineClass(name, classFile, 0, classFile.length);
                    } else if (name.endsWith("ObjectHashCodeHelper")) {
                        ClassWriter cw = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
                        cw.visit(52, ACC_PUBLIC | ACC_SUPER, intl(name), null, "java/lang/Object", null);

                        {
                            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "testHelper", "(Ljava/lang/Object;)I", null, null);
                            mv.visitCode();
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitMethodInsn(INVOKEINTERFACE, intl(test.getName()) + "$I", "hashCode", "()I", true);
                            mv.visitInsn(IRETURN);
                            mv.visitMaxs(0, 0);
                            mv.visitEnd();
                        }

                        byte[] classFile = cw.toByteArray();
                        return defineClass(name, classFile, 0, classFile.length);
                    } else if (c == test || name.startsWith(test.getName())) {
                        try {
                            String path = name.replace('.', '/') + ".class";
                            byte[] classFile = getParent().getResourceAsStream(path).readAllBytes();
                            return defineClass(name, classFile, 0, classFile.length);
                        } catch (IOException e) {
                            throw new Error(e);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // ClassNotFoundException thrown if class not found
                    // from the non-null parent class loader
                }

                if (c == null) {
                    // If still not found, then invoke findClass in order
                    // to find the class.
                    c = findClass(name);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    public interface RunnableWithException {
        void run() throws Throwable;
    }

    public static void shouldThrow(Class<? extends Throwable> expectedException, RunnableWithException r) {
        try {
            r.run();
            throw new AssertionError("Exception not thrown: " + expectedException.getName());
        } catch(Throwable e) {
            if (expectedException == e.getClass()) {
                // success: proper exception is thrown
            } else {
                throw new Error(expectedException.getName() + " is expected", e);
            }
        }
    }

    public static MethodHandle unsafeCastMH(Class<?> cls) {
        try {
            MethodHandle mh = MethodHandles.identity(Object.class);
            return MethodHandles.explicitCastArguments(mh, mh.type().changeReturnType(cls));
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    static <T> T compute(Callable<T> c) {
        try {
            return c.call();
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
