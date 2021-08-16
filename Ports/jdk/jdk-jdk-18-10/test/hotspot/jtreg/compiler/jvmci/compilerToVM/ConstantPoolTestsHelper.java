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

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.testcases.MultipleAbstractImplementer;
import compiler.jvmci.common.testcases.MultipleImplementer2;
import compiler.jvmci.common.testcases.MultipleImplementersInterface;
import compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes;
import compiler.jvmci.compilerToVM.ConstantPoolTestCase.TestedCPEntry;
import jdk.internal.access.SharedSecrets;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.reflect.ConstantPool;
import jdk.internal.reflect.ConstantPool.Tag;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.runtime.JVMCI;
import sun.hotspot.WhiteBox;

import java.util.HashMap;
import java.util.Map;

import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_CLASS;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_FIELDREF;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_INTERFACEMETHODREF;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_INVOKEDYNAMIC;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_METHODHANDLE;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_METHODREF;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_METHODTYPE;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_STRING;

/**
 * Class contains hard-coded constant pool tables for dummy classes used for
 * jdk.vm.ci.hotspot.CompilerToVM constant pool methods
 */
public class ConstantPoolTestsHelper {

    public static final int NO_CP_CACHE_PRESENT = Integer.MAX_VALUE;
    private static final MetaAccessProvider metaAccess = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();

    public enum DummyClasses {
        DUMMY_CLASS(MultipleImplementer2.class, CP_MAP_FOR_CLASS),
        DUMMY_ABS_CLASS(MultipleAbstractImplementer.class, CP_MAP_FOR_ABS_CLASS),
        DUMMY_INTERFACE(MultipleImplementersInterface.class, CP_MAP_FOR_INTERFACE);

        private static final WhiteBox WB = WhiteBox.getWhiteBox();
        public final Class<?> klass;
        public final ConstantPool constantPoolSS;
        public final Map<ConstantTypes, TestedCPEntry[]> testedCP;

        DummyClasses(Class<?> klass, Map<ConstantTypes, TestedCPEntry[]> testedCP) {
            this.klass = klass;
            this.constantPoolSS = SharedSecrets.getJavaLangAccess().getConstantPool(klass);
            this.testedCP = testedCP;
        }

        public int getCPCacheIndex(int cpi) {
            int cacheLength = WB.getConstantPoolCacheLength(this.klass);
            int indexTag = WB.getConstantPoolCacheIndexTag();
            for (int cpci = indexTag; cpci < cacheLength + indexTag; cpci++) {
                if (WB.remapInstructionOperandFromCPCache(this.klass, cpci) == cpi) {
                    if (constantPoolSS.getTagAt(cpi).equals(Tag.INVOKEDYNAMIC)) {
                        return WB.encodeConstantPoolIndyIndex(cpci) + indexTag;
                    }
                    return cpci;
                }
            }
            return NO_CP_CACHE_PRESENT;
        }
    }

    /**
     * Obtain a resolved Java method declared by a given type.
     *
     * @param type the declaring type
     * @param the method's name
     *
     * Currently, the lookup is based only on the method's name
     * but not on the method's signature (i.e., the first method
     * with a matching name declared on {@code type} is returned).
     */
    private static ResolvedJavaMethod getMethod(ResolvedJavaType type, String methodName) {
        if (methodName.equals("<clinit>")) {
            return type.getClassInitializer();
        }

        if (methodName.equals("<init>")) {
            ResolvedJavaMethod[] initializers = type.getDeclaredConstructors();
            if (initializers.length >= 0) {
                return initializers[0];
            } else {
                throw new IllegalArgumentException();
            }
        }

        for (ResolvedJavaMethod method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }

        throw new IllegalArgumentException();
    }

    private static ResolvedJavaType getType(Class<?> clazz) {
        ResolvedJavaType type = metaAccess.lookupJavaType(clazz);
        type.initialize();
        return type;
    }

    private static final Map<ConstantTypes, TestedCPEntry[]> CP_MAP_FOR_CLASS = new HashMap<>();
    static {
        CP_MAP_FOR_CLASS.put(CONSTANT_CLASS,
                new TestedCPEntry[] {
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementersInterface", null, null),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2", null, null),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2$1", null, null),
                    new TestedCPEntry("java/lang/invoke/MethodHandles$Lookup", null, null),
                }
        );
        CP_MAP_FOR_CLASS.put(CONSTANT_FIELDREF,
                new TestedCPEntry[] {
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "intStaticField",
                                      "I",
                                      new byte[] {(byte) Opcodes.PUTSTATIC, (byte) Opcodes.GETSTATIC},
                                      Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "longStaticField",
                                      "J",
                                      new byte[] {(byte) Opcodes.PUTSTATIC, (byte) Opcodes.GETSTATIC},
                                      Opcodes.ACC_FINAL | Opcodes.ACC_STATIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "floatStaticField",
                                      "F",
                                      new byte[] {(byte) Opcodes.PUTSTATIC, (byte) Opcodes.GETSTATIC},
                                      Opcodes.ACC_VOLATILE | Opcodes.ACC_STATIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "doubleStaticField",
                                      "D",
                                      new byte[] {(byte) Opcodes.PUTSTATIC, (byte) Opcodes.GETSTATIC},
                                      Opcodes.ACC_STATIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "stringStaticField",
                                      "Ljava/lang/String;",
                                      new byte[] {(byte) Opcodes.PUTSTATIC, (byte) Opcodes.GETSTATIC},
                                      Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "objectStaticField",
                                      "Ljava/lang/Object;",
                                      new byte[] {(byte) Opcodes.PUTSTATIC, (byte) Opcodes.GETSTATIC},
                                      Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "intField",
                                      "I",
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      Opcodes.ACC_PUBLIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "longField",
                                      "J",
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      Opcodes.ACC_PRIVATE),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "floatField",
                                      "F",
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      Opcodes.ACC_PROTECTED),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "doubleField",
                                      "D",
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      Opcodes.ACC_TRANSIENT),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "objectField",
                                      "Ljava/lang/Object;",
                                      new ResolvedJavaMethod[] { getMethod(getType(MultipleImplementer2.class), "<init>"), null },
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      Opcodes.ACC_FINAL),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "stringField",
                                      "Ljava/lang/String;",
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      Opcodes.ACC_VOLATILE),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "stringFieldEmpty",
                                      "Ljava/lang/String;",
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      0),
                }
        );
        CP_MAP_FOR_CLASS.put(CONSTANT_METHODREF,
                new TestedCPEntry[] {
                    new TestedCPEntry("java/lang/System",
                                      "getProperties",
                                      "()Ljava/util/Properties;",
                                      new byte[] {(byte) Opcodes.INVOKESTATIC}),
                    new TestedCPEntry("java/util/HashMap",
                                      "<init>",
                                      "()V",
                                      new byte[] {(byte) Opcodes.INVOKESPECIAL}),
                    new TestedCPEntry("java/lang/Object",
                                      "toString",
                                      "()Ljava/lang/String;",
                                      new byte[] {(byte) Opcodes.INVOKESPECIAL,
                                      (byte) Opcodes.INVOKEVIRTUAL}),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2$1",
                                      "<init>",
                                      "(Lcompiler/jvmci/common/testcases/MultipleImplementer2;)V",
                                      new byte[0]),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer$1",
                                      "run",
                                      "()V",
                                      new byte[0]),
                }
        );
        CP_MAP_FOR_CLASS.put(CONSTANT_INTERFACEMETHODREF,
                new TestedCPEntry[] {
                    new TestedCPEntry("java/util/Map",
                                      "put",
                                      "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                                      new byte[] {(byte) Opcodes.INVOKEINTERFACE}),
                    new TestedCPEntry("java/util/Map",
                                      "remove",
                                      "(Ljava/lang/Object;)Ljava/lang/Object;",
                                      new byte[] {(byte) Opcodes.INVOKEINTERFACE}),
                }
        );
        CP_MAP_FOR_CLASS.put(CONSTANT_STRING,
                new TestedCPEntry[] {
                    new TestedCPEntry(null, "Message", null),
                    new TestedCPEntry(null, "", null),
                }
        );
        CP_MAP_FOR_CLASS.put(CONSTANT_METHODHANDLE,
                new TestedCPEntry[] {
                    new TestedCPEntry("java/lang/invoke/LambdaMetafactory",
                                      "metafactory",
                                      "(Ljava/lang/invoke/MethodHandles$Lookup;"
                                              + "Ljava/lang/String;"
                                              + "Ljava/lang/invoke/MethodType;"
                                              + "Ljava/lang/invoke/MethodType;"
                                              + "Ljava/lang/invoke/MethodHandle;"
                                              + "Ljava/lang/invoke/MethodType;)"
                                              + "Ljava/lang/invoke/CallSite;",
                                      null),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementer2",
                                      "testMethod",
                                      "()V"),
                }
        );
        CP_MAP_FOR_CLASS.put(CONSTANT_METHODTYPE,
                new TestedCPEntry[] {
                    new TestedCPEntry(null, null, "()V"),
                }
        );
        CP_MAP_FOR_CLASS.put(CONSTANT_INVOKEDYNAMIC,
                new TestedCPEntry[] {
                    new TestedCPEntry(null,
                                     "run",
                                     "(Lcompiler/jvmci/common/testcases/MultipleImplementer2;)"
                                             + "Ljava/lang/Runnable;"),
                }
        );
    }

    private static final Map<ConstantTypes, TestedCPEntry[]> CP_MAP_FOR_ABS_CLASS
            = new HashMap<>();
    static {
        CP_MAP_FOR_ABS_CLASS.put(CONSTANT_CLASS,
                new TestedCPEntry[] {
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementersInterface", null, null),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer", null, null),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer$1", null, null),
                    new TestedCPEntry("java/lang/invoke/MethodHandles$Lookup", null, null),
                }
        );
        CP_MAP_FOR_ABS_CLASS.put(CONSTANT_FIELDREF,
                new TestedCPEntry[] {
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "intStaticField",
                                      "I",
                                      new byte[] {(byte) Opcodes.PUTSTATIC, (byte) Opcodes.GETSTATIC},
                                      Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "longStaticField",
                                      "J",
                                      new byte[] {(byte) Opcodes.PUTSTATIC, (byte) Opcodes.GETSTATIC},
                                      Opcodes.ACC_FINAL | Opcodes.ACC_STATIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "floatStaticField",
                                      "F",
                                      new byte[] {(byte) Opcodes.PUTSTATIC, (byte) Opcodes.GETSTATIC},
                                      Opcodes.ACC_VOLATILE | Opcodes.ACC_STATIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "doubleStaticField",
                                      "D",
                                      new byte[] {(byte) Opcodes.PUTSTATIC, (byte) Opcodes.GETSTATIC},
                                      Opcodes.ACC_STATIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "stringStaticField",
                                      "Ljava/lang/String;",
                                      new byte[] {(byte) Opcodes.PUTSTATIC, (byte) Opcodes.GETSTATIC},
                                      Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "objectStaticField",
                                      "Ljava/lang/Object;",
                                      new byte[] {(byte) Opcodes.PUTSTATIC, (byte) Opcodes.GETSTATIC},
                                      Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "intField",
                                      "I",
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      Opcodes.ACC_PUBLIC),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "longField",
                                      "J",
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      Opcodes.ACC_PRIVATE),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "floatField",
                                      "F",
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      Opcodes.ACC_PROTECTED),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "doubleField",
                                      "D",
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      Opcodes.ACC_TRANSIENT),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "objectField",
                                      "Ljava/lang/Object;",
                                      new ResolvedJavaMethod[] { getMethod(getType(MultipleAbstractImplementer.class), "<init>"), null },
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      Opcodes.ACC_FINAL),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "stringField",
                                      "Ljava/lang/String;",
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      Opcodes.ACC_VOLATILE),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "stringFieldEmpty",
                                      "Ljava/lang/String;",
                                      new byte[] {(byte) Opcodes.PUTFIELD | (byte) Opcodes.GETFIELD},
                                      0),
                }
        );
        CP_MAP_FOR_ABS_CLASS.put(CONSTANT_METHODREF,
                new TestedCPEntry[] {
                    new TestedCPEntry("java/lang/System",
                                      "getProperties",
                                      "()Ljava/util/Properties;",
                                      new byte[] {(byte) Opcodes.INVOKESTATIC}),
                    new TestedCPEntry("java/util/HashMap",
                                      "<init>",
                                      "()V",
                                      new byte[] {(byte) Opcodes.INVOKESPECIAL}),
                    new TestedCPEntry("java/lang/Object",
                                      "toString",
                                      "()Ljava/lang/String;",
                                      new byte[] {(byte) Opcodes.INVOKESPECIAL,
                                      (byte) Opcodes.INVOKEVIRTUAL}),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer$1",
                                      "<init>",
                                      "(Lcompiler/jvmci/common/testcases/MultipleAbstractImplementer;)V",
                                      new byte[0]),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer$1",
                                      "run",
                                      "()V",
                                      new byte[0]),
                }
        );
        CP_MAP_FOR_ABS_CLASS.put(CONSTANT_INTERFACEMETHODREF,
                new TestedCPEntry[] {
                    new TestedCPEntry("java/util/Map",
                                      "put",
                                      "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                                      new byte[] {(byte) Opcodes.INVOKEINTERFACE}),
                    new TestedCPEntry("java/util/Map",
                                      "remove",
                                      "(Ljava/lang/Object;)Ljava/lang/Object;",
                                      new byte[] {(byte) Opcodes.INVOKEINTERFACE}),
                }
        );
        CP_MAP_FOR_ABS_CLASS.put(CONSTANT_STRING,
                new TestedCPEntry[] {
                    new TestedCPEntry(null, "Message", null),
                    new TestedCPEntry(null, "", null),
                }
        );
        CP_MAP_FOR_ABS_CLASS.put(CONSTANT_METHODHANDLE,
                new TestedCPEntry[] {
                    new TestedCPEntry("java/lang/invoke/LambdaMetafactory",
                                      "metafactory",
                                      "(Ljava/lang/invoke/MethodHandles$Lookup;"
                                              + "Ljava/lang/String;"
                                              + "Ljava/lang/invoke/MethodType;"
                                              + "Ljava/lang/invoke/MethodType;"
                                              + "Ljava/lang/invoke/MethodHandle;"
                                              + "Ljava/lang/invoke/MethodType;)"
                                              + "Ljava/lang/invoke/CallSite;",
                                      null),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer",
                                      "testMethod",
                                      "()V"),
                }
        );
        CP_MAP_FOR_ABS_CLASS.put(CONSTANT_METHODTYPE,
                new TestedCPEntry[] {
                    new TestedCPEntry(null, null, "()V"),
                }
        );
        CP_MAP_FOR_ABS_CLASS.put(CONSTANT_INVOKEDYNAMIC,
                new TestedCPEntry[] {
                    new TestedCPEntry(null,
                                      "run",
                                      "(Lcompiler/jvmci/common/testcases/MultipleAbstractImplementer;)"
                                              + "Ljava/lang/Runnable;"),
                }
        );
    }

    private static final Map<ConstantTypes, TestedCPEntry[]> CP_MAP_FOR_INTERFACE
            = new HashMap<>();
    static {
        CP_MAP_FOR_INTERFACE.put(CONSTANT_CLASS,
                new TestedCPEntry[] {
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementersInterface", null, null),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementersInterface$1", null, null),
                    new TestedCPEntry("java/lang/Object", null, null),
                    new TestedCPEntry("java/lang/invoke/MethodHandles$Lookup", null, null),
                }
        );
        CP_MAP_FOR_INTERFACE.put(CONSTANT_FIELDREF,
                new TestedCPEntry[] {
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementersInterface",
                                      "OBJECT_CONSTANT",
                                      "Ljava/lang/Object;",
                                      new ResolvedJavaMethod[] { getMethod(getType(MultipleImplementersInterface.class), "<clinit>"), null },
                                      new byte[] {(byte) Opcodes.PUTSTATIC, (byte) Opcodes.GETSTATIC},
                                      Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC),
                }
        );
        CP_MAP_FOR_INTERFACE.put(CONSTANT_METHODREF,
                new TestedCPEntry[] {
                    new TestedCPEntry("java/lang/System",
                                      "getProperties",
                                      "()Ljava/util/Properties;",
                                      new byte[] {(byte) Opcodes.INVOKESTATIC}),
                    new TestedCPEntry("java/util/HashMap",
                                      "<init>",
                                      "()V",
                                      new byte[] {(byte) Opcodes.INVOKESPECIAL}),
                    new TestedCPEntry("java/lang/Object",
                                      "toString",
                                      "()Ljava/lang/String;",
                                      new byte[] {(byte) Opcodes.INVOKEVIRTUAL}),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer$1",
                                      "<init>",
                                      "(Lcompiler/jvmci/common/testcases/MultipleAbstractImplementer;)V",
                                      new byte[0]),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleAbstractImplementer$1",
                                      "run",
                                      "()V",
                                      new byte[0]),
                }
        );
        CP_MAP_FOR_INTERFACE.put(CONSTANT_INTERFACEMETHODREF,
                new TestedCPEntry[] {
                    new TestedCPEntry("java/util/Map",
                                      "put",
                                      "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                                      new byte[] {(byte) Opcodes.INVOKEINTERFACE}),
                    new TestedCPEntry("java/util/Map",
                                      "remove",
                                      "(Ljava/lang/Object;)Ljava/lang/Object;",
                                      new byte[] {(byte) Opcodes.INVOKEINTERFACE}),
                }
        );
        CP_MAP_FOR_INTERFACE.put(CONSTANT_STRING,
                new TestedCPEntry[] {
                    new TestedCPEntry(null, "Hello", null),
                    new TestedCPEntry(null, "", null),
                }
        );
        CP_MAP_FOR_INTERFACE.put(CONSTANT_METHODHANDLE,
                new TestedCPEntry[] {
                    new TestedCPEntry("java/lang/invoke/LambdaMetafactory",
                                      "metafactory",
                                      "(Ljava/lang/invoke/MethodHandles$Lookup;"
                                              + "Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                                              + "Ljava/lang/invoke/MethodType;"
                                              + "Ljava/lang/invoke/MethodHandle;"
                                              + "Ljava/lang/invoke/MethodType;)"
                                              + "Ljava/lang/invoke/CallSite;"),
                    new TestedCPEntry("compiler/jvmci/common/testcases/MultipleImplementersInterface",
                                      "defaultMethod",
                                      "()V"),
                }
        );
        CP_MAP_FOR_INTERFACE.put(CONSTANT_METHODTYPE,
                new TestedCPEntry[] {
                    new TestedCPEntry(null, null, "()V"),
                }
        );
        CP_MAP_FOR_INTERFACE.put(CONSTANT_INVOKEDYNAMIC,
                new TestedCPEntry[] {
                    new TestedCPEntry(null,
                                      "run",
                                      "(Lcompiler/jvmci/common/testcases/MultipleImplementersInterface;)"
                                              + "Ljava/lang/Runnable;"),
                }
        );
    }
}
