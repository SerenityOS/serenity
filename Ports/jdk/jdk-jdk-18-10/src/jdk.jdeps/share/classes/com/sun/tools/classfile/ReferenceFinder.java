/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.classfile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.sun.tools.classfile.Instruction.TypeKind;
import static com.sun.tools.classfile.ConstantPool.*;

/**
 * A utility class to find where in a ClassFile references
 * a {@link CONSTANT_Methodref_info method},
 * a {@link CONSTANT_InterfaceMethodref_info interface method},
 * or a {@link CONSTANT_Fieldref_info field}.
 */
public final class ReferenceFinder {
    /**
     * Filter for ReferenceFinder of what constant pool entries for reference lookup.
     */
    public interface Filter {
        /**
         * Decides if the given CPRefInfo entry should be accepted or filtered.
         *
         * @param cpool  ConstantPool of the ClassFile being parsed
         * @param cpref  constant pool entry representing a reference to
         *               a fields method, and interface method.
         * @return {@code true} if accepted; otherwise {@code false}
         */
        boolean accept(ConstantPool cpool, CPRefInfo cpref);
    }

    /**
     * Visitor of individual method of a ClassFile that references the
     * accepted field, method, or interface method references.
     */
    public interface Visitor {
        /**
         * Invoked for a method containing one or more accepted CPRefInfo entries
         *
         * @param cf      ClassFile
         * @param method  Method that does the references the accepted references
         * @param refs    Accepted constant pool method/field reference
         */
        void visit(ClassFile cf, Method method, List<CPRefInfo> refConstantPool);
    }

    private final Filter filter;
    private final Visitor visitor;

    /**
     * Constructor.
     */
    public ReferenceFinder(Filter filter, Visitor visitor) {
        this.filter = Objects.requireNonNull(filter);
        this.visitor = Objects.requireNonNull(visitor);
    }

    /**
     * Parses a given ClassFile and invoke the visitor if there is any reference
     * to the constant pool entries referencing field, method, or
     * interface method that are accepted. This method will return
     * {@code true} if there is one or more accepted constant pool entries
     * to lookup; otherwise, it will return {@code false}.
     *
     * @param  cf  ClassFile
     * @return {@code true} if the given class file is processed to lookup
     *         references
     * @throws ConstantPoolException if an error of the constant pool
     */
    public boolean parse(ClassFile cf) throws ConstantPoolException {
        List<Integer> cprefs = new ArrayList<>();
        int index = 1;
        for (ConstantPool.CPInfo cpInfo : cf.constant_pool.entries()) {
            if (cpInfo.accept(cpVisitor, cf.constant_pool)) {
                cprefs.add(index);
            }
            index += cpInfo.size();
        }

        if (cprefs.isEmpty()) {
            return false;
        }

        for (Method m : cf.methods) {
            Set<Integer> ids = new HashSet<>();
            Code_attribute c_attr = (Code_attribute) m.attributes.get(Attribute.Code);
            if (c_attr != null) {
                for (Instruction instr : c_attr.getInstructions()) {
                    int idx = instr.accept(codeVisitor, cprefs);
                    if (idx > 0) {
                        ids.add(idx);
                    }
                }
            }
            if (ids.size() > 0) {
                List<CPRefInfo> refInfos = new ArrayList<>(ids.size());
                for (int id : ids) {
                    refInfos.add(CPRefInfo.class.cast(cf.constant_pool.get(id)));
                }
                visitor.visit(cf, m, refInfos);
            }
        }
        return true;
    }

    private ConstantPool.Visitor<Boolean,ConstantPool> cpVisitor =
            new ConstantPool.Visitor<Boolean,ConstantPool>()
    {
        public Boolean visitClass(CONSTANT_Class_info info, ConstantPool cpool) {
            return false;
        }

        public Boolean visitFieldref(CONSTANT_Fieldref_info info, ConstantPool cpool) {
            return filter.accept(cpool, info);
        }

        public Boolean visitDouble(CONSTANT_Double_info info, ConstantPool cpool) {
            return false;
        }

        public Boolean visitFloat(CONSTANT_Float_info info, ConstantPool cpool) {
            return false;
        }

        public Boolean visitInteger(CONSTANT_Integer_info info, ConstantPool cpool) {
            return false;
        }

        public Boolean visitInterfaceMethodref(CONSTANT_InterfaceMethodref_info info, ConstantPool cpool) {
            return filter.accept(cpool, info);
        }

        public Boolean visitInvokeDynamic(CONSTANT_InvokeDynamic_info info, ConstantPool cpool) {
            return false;
        }

        @Override
        public Boolean visitDynamicConstant(CONSTANT_Dynamic_info info, ConstantPool constantPool) {
            return false;
        }

        public Boolean visitLong(CONSTANT_Long_info info, ConstantPool cpool) {
            return false;
        }

        public Boolean visitMethodHandle(CONSTANT_MethodHandle_info info, ConstantPool cpool) {
            return false;
        }

        public Boolean visitMethodref(CONSTANT_Methodref_info info, ConstantPool cpool) {
            return filter.accept(cpool, info);
        }

        public Boolean visitMethodType(CONSTANT_MethodType_info info, ConstantPool cpool) {
            return false;
        }

        public Boolean visitModule(CONSTANT_Module_info info, ConstantPool cpool) {
            return false;
        }

        public Boolean visitNameAndType(CONSTANT_NameAndType_info info, ConstantPool cpool) {
            return false;
        }

        public Boolean visitPackage(CONSTANT_Package_info info, ConstantPool cpool) {
            return false;
        }

        public Boolean visitString(CONSTANT_String_info info, ConstantPool cpool) {
            return false;
        }

        public Boolean visitUtf8(CONSTANT_Utf8_info info, ConstantPool cpool) {
            return false;
        }
    };

    private Instruction.KindVisitor<Integer, List<Integer>> codeVisitor =
            new Instruction.KindVisitor<Integer, List<Integer>>()
    {
        public Integer visitNoOperands(Instruction instr, List<Integer> p) {
            return 0;
        }

        public Integer visitArrayType(Instruction instr, TypeKind kind, List<Integer> p) {
            return 0;
        }

        public Integer visitBranch(Instruction instr, int offset, List<Integer> p) {
            return 0;
        }

        public Integer visitConstantPoolRef(Instruction instr, int index, List<Integer> p) {
            return p.contains(index) ? index : 0;
        }

        public Integer visitConstantPoolRefAndValue(Instruction instr, int index, int value, List<Integer> p) {
            return p.contains(index) ? index : 0;
        }

        public Integer visitLocal(Instruction instr, int index, List<Integer> p) {
            return 0;
        }

        public Integer visitLocalAndValue(Instruction instr, int index, int value, List<Integer> p) {
            return 0;
        }

        public Integer visitLookupSwitch(Instruction instr, int default_, int npairs, int[] matches, int[] offsets, List<Integer> p) {
            return 0;
        }

        public Integer visitTableSwitch(Instruction instr, int default_, int low, int high, int[] offsets, List<Integer> p) {
            return 0;
        }

        public Integer visitValue(Instruction instr, int value, List<Integer> p) {
            return 0;
        }

        public Integer visitUnknown(Instruction instr, List<Integer> p) {
            return 0;
        }
    };
}

