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

package compiler.jvmci.common;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.test.lib.Utils;
import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotNmethod;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class CTVMUtilities {
    /*
     * A method to return HotSpotResolvedJavaMethod object using class object
     * and method as input
     */
    public static HotSpotResolvedJavaMethod getResolvedMethod(Class<?> cls,
            Executable method) {
        if (!(method instanceof Method || method instanceof Constructor)) {
            throw new Error("wrong executable type " + method.getClass());
        }
        return CompilerToVMHelper.asResolvedJavaMethod(method);
    }

    public static HotSpotResolvedJavaMethod getResolvedMethod(
            Executable method) {
        return getResolvedMethod(method.getDeclaringClass(), method);
    }

    public static InstalledCode getInstalledCode(ResolvedJavaMethod method, String name, long address, long entryPoint) {
        return CompilerToVMHelper.getInstalledCode(method, name, address, entryPoint);
    }

    public static Map<Integer, Integer> getBciToLineNumber(Executable method) {
        Map<Integer, Integer> lineNumbers = new TreeMap<>();
        Class<?> aClass = method.getDeclaringClass();
        ClassReader cr;
        try {
            Module aModule = aClass.getModule();
            String name = aClass.getName();
            cr = new ClassReader(aModule.getResourceAsStream(
                    name.replace('.', '/') + ".class"));
        } catch (IOException e) {
                        throw new Error("TEST BUG: can read " + aClass.getName() + " : " + e, e);
        }
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);

        Map<Label, Integer> labels = new HashMap<>();
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor cv = new ClassVisitorForLabels(cw, labels, method);
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        labels.forEach((k, v) -> lineNumbers.put(k.getOffset(), v));
        boolean isEmptyMethod = Modifier.isAbstract(method.getModifiers())
                || Modifier.isNative(method.getModifiers());
        if (lineNumbers.isEmpty() && !isEmptyMethod) {
            throw new Error(method + " doesn't contains the line numbers table "
                    +"(the method marked neither abstract nor native)");
        }
        return lineNumbers;
    }

    private static class ClassVisitorForLabels extends ClassVisitor {
        private final Map<Label, Integer> lineNumbers;
        private final String targetName;
        private final String targetDesc;

        public ClassVisitorForLabels(ClassWriter cw, Map<Label, Integer> lines,
                                     Executable target) {
            super(Opcodes.ASM7, cw);
            this.lineNumbers = lines;

            StringBuilder builder = new StringBuilder("(");
            for (Parameter parameter : target.getParameters()) {
                builder.append(Utils.toJVMTypeSignature(parameter.getType()));
            }
            builder.append(")");
            if (target instanceof Constructor) {
                targetName = "<init>";
                builder.append("V");
            } else {
                targetName = target.getName();
                builder.append(Utils.toJVMTypeSignature(
                        ((Method) target).getReturnType()));
            }
            targetDesc = builder.toString();
        }

        @Override
        public final MethodVisitor visitMethod(int access, String name,
                                               String desc, String signature,
                                               String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature,
                    exceptions);
            if (targetDesc.equals(desc) && targetName.equals(name)) {
                return new MethodVisitor(Opcodes.ASM7, mv) {
                    @Override
                    public void visitLineNumber(int i, Label label) {
                        super.visitLineNumber(i, label);
                        lineNumbers.put(label, i);
                    }
                };
            }
            return  mv;
        }
    }
}
