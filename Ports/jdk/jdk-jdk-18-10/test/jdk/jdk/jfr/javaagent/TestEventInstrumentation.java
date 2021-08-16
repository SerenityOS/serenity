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
package jdk.jfr.javaagent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Verify that a subclass of the JFR Event class
 *          can be successfully instrumented.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          jdk.jartool/sun.tools.jar
 * @build jdk.jfr.javaagent.InstrumentationEventCallback
 *        jdk.jfr.javaagent.TestEventInstrumentation
 * @run driver jdk.test.lib.util.JavaAgentBuilder
 *             jdk.jfr.javaagent.TestEventInstrumentation TestEventInstrumentation.jar
 *             Can-Redefine-Classes:true Can-Retransform-Classes:true
 * @run main/othervm -javaagent:TestEventInstrumentation.jar
 *      jdk.jfr.javaagent.TestEventInstrumentation
 */
public class TestEventInstrumentation {
    private static Instrumentation instrumentation = null;
    private static TestEventInstrumentation testTransformer = null;
    private static Exception transformException = null;

    public static class TestEvent extends Event {
    }

    public static void main(String[] args) throws Throwable {
        // loads test event class, run empty constructor w/o instrumentation
        TestEvent event = new TestEvent();

        // add instrumentation and test an instrumented constructor
        instrumentation.addTransformer(new Transformer(), true);
        instrumentation.retransformClasses(TestEvent.class);
        event = new TestEvent();
        Asserts.assertTrue(InstrumentationEventCallback.wasCalled());

        // record test event with instrumented constructor, verify it is recorded
        InstrumentationEventCallback.clear();
        try (Recording r = new Recording()) {
            r.enable(TestEvent.class);
            r.start();
            new TestEvent().commit();
            Asserts.assertTrue(InstrumentationEventCallback.wasCalled());
            Path rf = Paths.get("", "recording.jfr");
            r.dump(rf);
            Asserts.assertFalse(RecordingFile.readAllEvents(rf).isEmpty());
        }
    }

    private static void log(String msg) {
        System.out.println(msg);
    }

    // ======================== Java agent used to transform classes
    public static void premain(String args, Instrumentation inst) throws Exception {
        instrumentation = inst;
    }

    static class Transformer implements ClassFileTransformer {
        public byte[] transform(ClassLoader classLoader, String className,
                                Class<?> classBeingRedefined, ProtectionDomain pd,
                                byte[] bytes) throws IllegalClassFormatException {
            byte[] result = null;
            try {
                // Check if this class should be instrumented.
                if (!className.contains("TestEventInstrumentation$TestEvent")) {
                    return null;
                }

                ClassReader reader = new ClassReader(bytes);
                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                CallbackClassVisitor classVisitor = new CallbackClassVisitor(writer);

                // visit the reader's class by the classVisitor
                reader.accept(classVisitor, 0);
                result = writer.toByteArray();
            } catch (Exception e) {
                log("Exception occured in transform(): " + e.getMessage());
                e.printStackTrace(System.out);
                transformException = e;
            }
            return result;
        }

        private static class CallbackClassVisitor extends ClassVisitor {
            private String className;

            public CallbackClassVisitor(ClassVisitor cv) {
                super(Opcodes.ASM7, cv);
            }

            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                // visit the header of the class - called per class header visit
                cv.visit(version, access, name, signature, superName, interfaces);
                className = name;
            }

            @Override
            public MethodVisitor visitMethod(
                                             int access, String methodName, String desc,
                                             String signature, String[] exceptions) {
                // called for each method in a class
                boolean isInstrumentedMethod = methodName.contains("<init>");
                MethodVisitor mv = cv.visitMethod(access, methodName, desc, signature, exceptions);
                if (isInstrumentedMethod) {
                    mv = new CallbackMethodVisitor(mv);
                    log("instrumented <init> in class " + className);
                }
                return mv;
            }
        }

        public static class CallbackMethodVisitor extends MethodVisitor {
            public CallbackMethodVisitor(MethodVisitor mv) {
                super(Opcodes.ASM7, mv);
            }

            @Override
            public void visitCode() {
                mv.visitCode();
                String methodDescr = Type.getMethodDescriptor(Type.VOID_TYPE, Type.VOID_TYPE);
                String className = InstrumentationEventCallback.class.getName().replace('.', '/');
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "callback", "()V", false);
            }
        }
    }
}
