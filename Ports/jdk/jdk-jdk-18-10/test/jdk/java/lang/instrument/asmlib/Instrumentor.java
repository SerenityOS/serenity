/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package asmlib;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

import java.util.function.Consumer;
import jdk.internal.org.objectweb.asm.Type;

public class Instrumentor {
    public static class InstrHelper {
        private final MethodVisitor mv;
        private final String name;

        InstrHelper(MethodVisitor mv, String name) {
            this.mv = mv;
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public void invokeStatic(String owner, String name, String desc, boolean itf) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc, itf);
        }

        public void invokeSpecial(String owner, String name, String desc) {
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, desc, false);
        }

        public void invokeVirtual(String owner, String name, String desc) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, desc, false);
        }

        public void push(int val) {
            if (val >= -1 && val <= 5) {
                mv.visitInsn(Opcodes.ICONST_0 + val);
            } else if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
                mv.visitIntInsn(Opcodes.BIPUSH, val);
            } else if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
                mv.visitIntInsn(Opcodes.SIPUSH, val);
            } else {
                mv.visitLdcInsn(val);
            }
        }

        public void push(Object val) {
            mv.visitLdcInsn(val);
        }

        public void println(String s) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(System.class), "out", Type.getDescriptor(PrintStream.class));
            mv.visitLdcInsn(s);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PrintStream.class), "println", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);
        }
    }

    public static Instrumentor instrFor(byte[] classData) {
        return new Instrumentor(classData);
    }


    private final ClassReader cr;
    private final ClassWriter output;
    private ClassVisitor instrumentingVisitor = null;
    private final AtomicInteger matches = new AtomicInteger(0);

    private Instrumentor(byte[] classData) {
        cr = new ClassReader(classData);
        output = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        instrumentingVisitor = output;
    }

    public synchronized Instrumentor addMethodEntryInjection(String methodName, Consumer<InstrHelper> injector) {
        instrumentingVisitor = new ClassVisitor(Opcodes.ASM7, instrumentingVisitor) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if (name.equals(methodName)) {
                    matches.getAndIncrement();

                    mv = new MethodVisitor(Opcodes.ASM7, mv) {
                        @Override
                        public void visitCode() {
                            injector.accept(new InstrHelper(mv, name));
                        }
                    };
                }
                return mv;
            }
        };
        return this;
    }

    public synchronized Instrumentor addNativeMethodTrackingInjection(String prefix, Consumer<InstrHelper> injector) {
        instrumentingVisitor = new ClassVisitor(Opcodes.ASM7, instrumentingVisitor) {
            private final Set<Consumer<ClassVisitor>> wmGenerators = new HashSet<>();
            private String className;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.className = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }


            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if ((access & Opcodes.ACC_NATIVE) != 0) {
                    matches.getAndIncrement();

                    String newName = prefix + name;
                    wmGenerators.add((v)->{
                        MethodVisitor mv = v.visitMethod(access & ~Opcodes.ACC_NATIVE, name, desc, signature, exceptions);
                        mv.visitCode();
                        injector.accept(new InstrHelper(mv, name));
                        Type[] argTypes = Type.getArgumentTypes(desc);
                        Type retType = Type.getReturnType(desc);

                        boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
                        if (!isStatic) {
                            mv.visitIntInsn(Opcodes.ALOAD, 0); // load "this"
                        }

                        // load the method parameters
                        if (argTypes.length > 0) {
                            int ptr = isStatic ? 0 : 1;
                            for(Type argType : argTypes) {
                                mv.visitIntInsn(argType.getOpcode(Opcodes.ILOAD), ptr);
                                ptr += argType.getSize();
                            }
                        }

                        mv.visitMethodInsn(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKESPECIAL, className, newName, desc, false);
                        mv.visitInsn(retType.getOpcode(Opcodes.IRETURN));

                        mv.visitMaxs(1, 1); // dummy call; let ClassWriter to deal with this
                        mv.visitEnd();
                    });
                    return super.visitMethod(access, newName, desc, signature, exceptions);
                }
                return super.visitMethod(access, name, desc, signature, exceptions);
            }

            @Override
            public void visitEnd() {
                wmGenerators.stream().forEach((e) -> {
                    e.accept(cv);
                });
                super.visitEnd();
            }
        };

        return this;
    }

    public synchronized byte[] apply() {
        cr.accept(instrumentingVisitor, ClassReader.SKIP_DEBUG + ClassReader.EXPAND_FRAMES);

        return matches.get() == 0 ? null : output.toByteArray();
    }
}
