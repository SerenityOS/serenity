/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8042235
 * @summary redefining method used by multiple MethodHandles crashes VM
 * @library /
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.compiler
 *          java.instrument
 *          jdk.attach
 * @requires vm.jvmti
 *
 * @run main/othervm -Djdk.attach.allowAttachSelf compiler.jsr292.RedefineMethodUsedByMultipleMethodHandles
 */

package compiler.jsr292;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class RedefineMethodUsedByMultipleMethodHandles {

    static class Foo {
        public static Object getName() {
            return "foo";
        }
    }

    public static void main(String[] args) throws Throwable {

        Lookup lookup = MethodHandles.lookup();
        Method fooMethod = Foo.class.getDeclaredMethod("getName");

        // fooMH2 displaces fooMH1 from the MemberNamesTable
        MethodHandle fooMH1 = lookup.unreflect(fooMethod);
        MethodHandle fooMH2 = lookup.unreflect(fooMethod);

        System.out.println("fooMH1.invoke = " + fooMH1.invokeExact());
        System.out.println("fooMH2.invoke = " + fooMH2.invokeExact());

        // Redefining Foo.getName() causes vmtarget to be updated
        // in fooMH2 but not fooMH1
        redefineFoo();

        // Full GC causes fooMH1.vmtarget to be deallocated
        System.gc();

        // Calling fooMH1.vmtarget crashes the VM
        System.out.println("fooMH1.invoke = " + fooMH1.invokeExact());
    }

    /**
     * Adds the class file bytes for {@code c} to {@code jar}.
     */
    static void add(JarOutputStream jar, Class<?> c) throws IOException {
        String classAsPath = c.getName().replace('.', '/') + ".class";
        jar.putNextEntry(new JarEntry(classAsPath));
        InputStream stream = c.getClassLoader().getResourceAsStream(classAsPath);

        int b;
        while ((b = stream.read()) != -1) {
            jar.write(b);
        }
    }

    static void redefineFoo() throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        Attributes mainAttrs = manifest.getMainAttributes();
        mainAttrs.putValue("Agent-Class", FooAgent.class.getName());
        mainAttrs.putValue("Can-Redefine-Classes", "true");
        mainAttrs.putValue("Can-Retransform-Classes", "true");

        // The jar file will be added to the system classloader search path.  It is not safe
        // to delete it while the JVM is running, so make sure to create it in the test
        // directory so it will be cleaned up by the test harness.
        Path jar = Files.createTempFile(Path.of(""), "myagent", ".jar");
        JarOutputStream jarStream = new JarOutputStream(new FileOutputStream(jar.toFile()), manifest);
        add(jarStream, FooAgent.class);
        add(jarStream, FooTransformer.class);
        jarStream.close();
        runAgent(jar);
    }

    public static void runAgent(Path agent) throws Exception {
        String pid = Long.toString(ProcessHandle.current().pid());
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        Class<?> c = Class.forName("com.sun.tools.attach.VirtualMachine", true, cl);
        Method attach = c.getDeclaredMethod("attach", String.class);
        Method loadAgent = c.getDeclaredMethod("loadAgent", String.class);
        Method detach = c.getDeclaredMethod("detach");
        Object vm = attach.invoke(null, pid);
        loadAgent.invoke(vm, agent.toString());
        detach.invoke(vm);
    }

    public static class FooAgent {

        public static void agentmain(@SuppressWarnings("unused") String args, Instrumentation inst) throws Exception {
            assert inst.isRedefineClassesSupported();
            assert inst.isRetransformClassesSupported();
            inst.addTransformer(new FooTransformer(), true);
            Class<?>[] classes = inst.getAllLoadedClasses();
            for (int i = 0; i < classes.length; i++) {
                Class<?> c = classes[i];
                if (c == Foo.class) {
                    inst.retransformClasses(new Class[]{c});
                }
            }
        }
    }

    static class FooTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader cl, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if (Foo.class.equals(classBeingRedefined)) {
                System.out.println("redefining " + classBeingRedefined);
                ClassReader cr = new ClassReader(classfileBuffer);
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
                ClassVisitor adapter = new ClassVisitor(Opcodes.ASM5, cw) {
                    @Override
                    public MethodVisitor visitMethod(int access, String base, String desc, String signature, String[] exceptions) {
                        MethodVisitor mv = cv.visitMethod(access, base, desc, signature, exceptions);
                        if (mv != null) {
                            mv = new MethodVisitor(Opcodes.ASM5, mv) {
                                @Override
                                public void visitLdcInsn(Object cst) {
                                    System.out.println("replacing \"" + cst + "\" with \"bar\"");
                                    mv.visitLdcInsn("bar");
                                }
                            };
                        }
                        return mv;
                    }
                };

                cr.accept(adapter, ClassReader.SKIP_FRAMES);
                cw.visitEnd();
                return cw.toByteArray();
            }
            return classfileBuffer;
        }
    }
}
