/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jfr.internal.instrument;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.commons.RemappingMethodAdapter;
import jdk.internal.org.objectweb.asm.commons.SimpleRemapper;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.jfr.internal.LogLevel;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.Logger;

/**
 * This class will merge (some) methods from one class into another one.
 *
 * @author Staffan Larsen
 */
@Deprecated
final class JIMethodMergeAdapter extends ClassVisitor {

    private final ClassNode cn;
    private final List<Method> methodFilter;
    private final Map<String, String> typeMap;

    /**
     * Methods in methodFilter that exist in cn will be merged into cv. If the method already exists,
     * the original method will be deleted.
     *
     * @param cv
     * @param cn - a ClassNode with Methods that will be merged into this class
     * @param methodFilter - only methods in this list will be merged
     * @param typeMappings - while merging, type references in the methods will be changed according to this map
     */
    public JIMethodMergeAdapter(ClassVisitor cv, ClassNode cn, List<Method> methodFilter, JITypeMapping[] typeMappings) {
        super(Opcodes.ASM7, cv);
        this.cn = cn;
        this.methodFilter = methodFilter;

        this.typeMap = new HashMap<>();
        for (JITypeMapping tm : typeMappings) {
            typeMap.put(tm.from().replace('.', '/'), tm.to().replace('.', '/'));
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        typeMap.put(cn.name, name);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if(methodInFilter(name, desc)) {
            // If the method is one that we will be replacing, delete the method
            Logger.log(LogTag.JFR_SYSTEM_BYTECODE, LogLevel.DEBUG, "Deleting " + name + desc);
            return null;
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        SimpleRemapper remapper = new SimpleRemapper(typeMap);
        for (MethodNode mn : cn.methods) {
            // Check if the method is in the list of methods to copy
            if (methodInFilter(mn.name, mn.desc)) {
                if (Logger.shouldLog(LogTag.JFR_SYSTEM_BYTECODE, LogLevel.DEBUG)) {
                    Logger.log(LogTag.JFR_SYSTEM_BYTECODE, LogLevel.DEBUG, "Copying method: " + mn.name + mn.desc);
                    Logger.log(LogTag.JFR_SYSTEM_BYTECODE, LogLevel.DEBUG, "   with mapper: " + typeMap);
                }

                String[] exceptions = new String[mn.exceptions.size()];
                mn.exceptions.toArray(exceptions);
                MethodVisitor mv = cv.visitMethod(mn.access, mn.name, mn.desc, mn.signature, exceptions);
                mn.instructions.resetLabels();
                mn.accept(new RemappingMethodAdapter(mn.access, mn.desc, mv, remapper));
            }
        }
        super.visitEnd();
    }

    private boolean methodInFilter(String name, String desc) {
        for(Method m : methodFilter) {
            if (m.getName().equals(name) && Type.getMethodDescriptor(m).equals(desc)) {
                return true;
            }
        }
        return false;
    }
}
