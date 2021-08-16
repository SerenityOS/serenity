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

package compiler.jvmci.compilerToVM;

import compiler.jvmci.compilerToVM.ConstantPoolTestsHelper.DummyClasses;
import jdk.internal.reflect.ConstantPool;
import jdk.internal.reflect.ConstantPool.Tag;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import sun.hotspot.WhiteBox;

import java.util.HashMap;
import java.util.Map;

import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_CLASS;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_DOUBLE;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_FIELDREF;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_FLOAT;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_INTEGER;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_INTERFACEMETHODREF;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_INVALID;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_INVOKEDYNAMIC;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_LONG;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_METHODHANDLE;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_METHODREF;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_METHODTYPE;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_NAMEANDTYPE;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_STRING;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_UTF8;

/**
 * Common class for jdk.vm.ci.hotspot.CompilerToVM constant pool tests
 */
public class ConstantPoolTestCase {

    private static final Map<Tag, ConstantTypes> TAG_TO_TYPE_MAP;
    static {
        TAG_TO_TYPE_MAP = new HashMap<>();
        TAG_TO_TYPE_MAP.put(Tag.CLASS, CONSTANT_CLASS);
        TAG_TO_TYPE_MAP.put(Tag.FIELDREF, CONSTANT_FIELDREF);
        TAG_TO_TYPE_MAP.put(Tag.METHODREF, CONSTANT_METHODREF);
        TAG_TO_TYPE_MAP.put(Tag.INTERFACEMETHODREF, CONSTANT_INTERFACEMETHODREF);
        TAG_TO_TYPE_MAP.put(Tag.STRING, CONSTANT_STRING);
        TAG_TO_TYPE_MAP.put(Tag.INTEGER, CONSTANT_INTEGER);
        TAG_TO_TYPE_MAP.put(Tag.FLOAT, CONSTANT_FLOAT);
        TAG_TO_TYPE_MAP.put(Tag.LONG, CONSTANT_LONG);
        TAG_TO_TYPE_MAP.put(Tag.DOUBLE, CONSTANT_DOUBLE);
        TAG_TO_TYPE_MAP.put(Tag.NAMEANDTYPE, CONSTANT_NAMEANDTYPE);
        TAG_TO_TYPE_MAP.put(Tag.UTF8, CONSTANT_UTF8);
        TAG_TO_TYPE_MAP.put(Tag.METHODHANDLE, CONSTANT_METHODHANDLE);
        TAG_TO_TYPE_MAP.put(Tag.METHODTYPE, CONSTANT_METHODTYPE);
        TAG_TO_TYPE_MAP.put(Tag.INVOKEDYNAMIC, CONSTANT_INVOKEDYNAMIC);
        TAG_TO_TYPE_MAP.put(Tag.INVALID, CONSTANT_INVALID);
    }
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private final Map<ConstantTypes, Validator> typeTests;

    public static enum ConstantTypes {
        CONSTANT_CLASS {
            @Override
            public TestedCPEntry getTestedCPEntry(DummyClasses dummyClass, int index) {
                ConstantPool constantPoolSS = dummyClass.constantPoolSS;
                checkIndex(constantPoolSS, index);
                Class<?> klass = constantPoolSS.getClassAt(index);
                String klassName = klass.getName();
                TestedCPEntry[] testedEntries = dummyClass.testedCP.get(this);
                for (TestedCPEntry entry : testedEntries) {
                    if (entry.klass.replaceAll("/", "\\.").equals(klassName)) {
                        return entry;
                    }
                }
                return null;
            }
        },
        CONSTANT_FIELDREF {
            @Override
            public TestedCPEntry getTestedCPEntry(DummyClasses dummyClass, int index) {
                return this.getTestedCPEntryForMethodAndField(dummyClass, index);
            }
        },
        CONSTANT_METHODREF {
            @Override
            public TestedCPEntry getTestedCPEntry(DummyClasses dummyClass, int index) {
                return this.getTestedCPEntryForMethodAndField(dummyClass, index);
            }
        },
        CONSTANT_INTERFACEMETHODREF {
            @Override
            public TestedCPEntry getTestedCPEntry(DummyClasses dummyClass, int index) {
                return this.getTestedCPEntryForMethodAndField(dummyClass, index);
            }
        },
        CONSTANT_STRING {
            @Override
            public TestedCPEntry getTestedCPEntry(DummyClasses dummyClass, int index) {
                ConstantPool constantPoolSS = dummyClass.constantPoolSS;
                checkIndex(constantPoolSS, index);
                String value = constantPoolSS.getStringAt(index);
                TestedCPEntry[] testedEntries = dummyClass.testedCP.get(this);
                for (TestedCPEntry entry : testedEntries) {
                    if (entry.name.equals(value)) {
                        return entry;
                    }
                }
                return null;
            }
        },
        CONSTANT_INTEGER,
        CONSTANT_FLOAT,
        CONSTANT_LONG,
        CONSTANT_DOUBLE,
        CONSTANT_NAMEANDTYPE,
        CONSTANT_UTF8,
        CONSTANT_METHODHANDLE,
        CONSTANT_METHODTYPE,
        CONSTANT_INVOKEDYNAMIC {
            @Override
            public TestedCPEntry getTestedCPEntry(DummyClasses dummyClass, int index) {
                ConstantPool constantPoolSS = dummyClass.constantPoolSS;
                checkIndex(constantPoolSS, index);
                int nameAndTypeIndex = constantPoolSS.getNameAndTypeRefIndexAt(index);
                String[] info = constantPoolSS.getNameAndTypeRefInfoAt(nameAndTypeIndex);
                TestedCPEntry[] testedEntries = dummyClass.testedCP.get(this);
                for (TestedCPEntry entry : testedEntries) {
                    if (info[0].equals(entry.name) && info[1].equals(entry.type)) {
                        return entry;
                    }
                }
                return null;
            }
        },
        CONSTANT_INVALID;

        public TestedCPEntry getTestedCPEntry(DummyClasses dummyClass, int index) {
            return null; // returning null by default
        }

        public TestedCPEntry[] getAllCPEntriesForType(DummyClasses dummyClass) {
            TestedCPEntry[] toReturn = dummyClass.testedCP.get(this);
            if (toReturn == null) {
                return new TestedCPEntry[0];
            }
            return dummyClass.testedCP.get(this);
        }

        protected TestedCPEntry getTestedCPEntryForMethodAndField(DummyClasses dummyClass, int index) {
            ConstantPool constantPoolSS = dummyClass.constantPoolSS;
            checkIndex(constantPoolSS, index);
            String[] info = constantPoolSS.getMemberRefInfoAt(index);
            TestedCPEntry[] testedEntries = dummyClass.testedCP.get(this);
            for (TestedCPEntry entry : testedEntries) {
                if (info[0].equals(entry.klass) && info[1].equals(entry.name) && info[2].equals(entry.type)) {
                    return entry;
                }
            }
            return null;
        }

        protected void checkIndex(ConstantPool constantPoolSS, int index) {
            ConstantPool.Tag tag = constantPoolSS.getTagAt(index);
            ConstantTypes type = mapTagToCPType(tag);
            if (!this.equals(type)) {
                String msg = String.format("TESTBUG: CP tag should be a %s, but is %s",
                                           this.name(),
                                           type.name());
               throw new Error(msg);
            }
        }
    }

    public static interface Validator {
        void validate(jdk.vm.ci.meta.ConstantPool constantPoolCTVM,
                      ConstantTypes cpType,
                      DummyClasses dummyClass,
                      int index);
    }

    public static class TestedCPEntry {
        public final String klass;
        public final String name;
        public final String type;
        public final ResolvedJavaMethod[] methods;
        public final byte[] opcodes;
        public final int accFlags;

        public TestedCPEntry(String klass, String name, String type, byte[] opcodes, int accFlags) {
                this(klass, name, type, null, opcodes, accFlags);
        }

        public TestedCPEntry(String klass, String name, String type, ResolvedJavaMethod[] methods, byte[] opcodes, int accFlags) {
            this.klass = klass;
            this.name = name;
            this.type = type;
            if (methods != null) {
                this.methods = new ResolvedJavaMethod[methods.length];
                System.arraycopy(methods, 0, this.methods, 0, methods.length);
            } else {
                this.methods = null;
            }
            if (opcodes != null) {
                this.opcodes = new byte[opcodes.length];
                System.arraycopy(opcodes, 0, this.opcodes, 0, opcodes.length);
            } else {
                this.opcodes = null;
            }
            this.accFlags = accFlags;
        }

        public TestedCPEntry(String klass, String name, String type, byte[] opcodes) {
            this(klass, name, type, opcodes, 0);
        }

        public TestedCPEntry(String klass, String name, String type) {
            this(klass, name, type, null, 0);
        }
    }

    public static ConstantTypes mapTagToCPType(Tag tag) {
        return TAG_TO_TYPE_MAP.get(tag);
    }

    public ConstantPoolTestCase(Map<ConstantTypes, Validator> typeTests) {
        this.typeTests = new HashMap<>();
        this.typeTests.putAll(typeTests);
    }

    public void test() {
        for (DummyClasses dummyClass : DummyClasses.values()) {
            boolean isCPCached = WB.getConstantPoolCacheLength(dummyClass.klass) > -1;
            System.out.printf("Testing dummy %s with constant pool cached = %b%n",
                              dummyClass.klass,
                              isCPCached);
            HotSpotResolvedObjectType holder = CompilerToVMHelper.fromObjectClass(dummyClass.klass);
            jdk.vm.ci.meta.ConstantPool constantPoolCTVM = holder.getConstantPool();
            ConstantPool constantPoolSS = dummyClass.constantPoolSS;
            for (int i = 0; i < constantPoolSS.getSize(); i++) {
                Tag tag = constantPoolSS.getTagAt(i);
                ConstantTypes cpType = mapTagToCPType(tag);
                if (!typeTests.keySet().contains(cpType)) {
                    continue;
                }
                typeTests.get(cpType).validate(constantPoolCTVM, cpType, dummyClass, i);
            }
        }
    }
}
