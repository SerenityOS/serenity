/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.hotspot;

import static jdk.vm.ci.hotspot.CompilerToVM.compilerToVM;
import static jdk.vm.ci.hotspot.HotSpotJVMCIRuntime.runtime;
import static jdk.vm.ci.hotspot.HotSpotVMConfig.config;
import static jdk.vm.ci.hotspot.UnsafeAccess.UNSAFE;

import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.common.NativeImageReinitialize;
import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaField;
import jdk.vm.ci.meta.JavaMethod;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Signature;
import jdk.vm.ci.meta.UnresolvedJavaField;
import jdk.vm.ci.meta.UnresolvedJavaMethod;
import jdk.vm.ci.meta.UnresolvedJavaType;

/**
 * Implementation of {@link ConstantPool} for HotSpot.
 */
public final class HotSpotConstantPool implements ConstantPool, MetaspaceHandleObject {

    /**
     * Subset of JVM bytecode opcodes used by {@link HotSpotConstantPool}.
     */
    public static class Bytecodes {
        public static final int LDC = 18; // 0x12
        public static final int LDC_W = 19; // 0x13
        public static final int LDC2_W = 20; // 0x14
        public static final int GETSTATIC = 178; // 0xB2
        public static final int PUTSTATIC = 179; // 0xB3
        public static final int GETFIELD = 180; // 0xB4
        public static final int PUTFIELD = 181; // 0xB5
        public static final int INVOKEVIRTUAL = 182; // 0xB6
        public static final int INVOKESPECIAL = 183; // 0xB7
        public static final int INVOKESTATIC = 184; // 0xB8
        public static final int INVOKEINTERFACE = 185; // 0xB9
        public static final int INVOKEDYNAMIC = 186; // 0xBA
        public static final int NEW = 187; // 0xBB
        public static final int NEWARRAY = 188; // 0xBC
        public static final int ANEWARRAY = 189; // 0xBD
        public static final int CHECKCAST = 192; // 0xC0
        public static final int INSTANCEOF = 193; // 0xC1
        public static final int MULTIANEWARRAY = 197; // 0xC5

        static boolean isInvoke(int opcode) {
            switch (opcode) {
                case INVOKEVIRTUAL:
                case INVOKESPECIAL:
                case INVOKESTATIC:
                case INVOKEINTERFACE:
                case INVOKEDYNAMIC:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * See: {@code Rewriter::maybe_rewrite_invokehandle}.
         */
        static boolean isInvokeHandleAlias(int opcode) {
            switch (opcode) {
                case INVOKEVIRTUAL:
                case INVOKESPECIAL:
                    return true;
                default:
                    return false;
            }
        }
    }

    static final class JvmConstant {
        private final int tag;
        private final String name;

        JvmConstant(int tag, String name) {
            this.tag = tag;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * {@code JVM_CONSTANT} constants used in the VM including both public and internal ones.
     */
    static final class JvmConstants {

        private final HotSpotVMConfig c = config();
        private final int externalMax = c.jvmConstantExternalMax;
        private final int internalMax = c.jvmConstantInternalMax;
        private final int internalMin = c.jvmConstantInternalMin;
        private final JvmConstant[] table = new JvmConstant[externalMax + 1 + (internalMax - internalMin) + 1];

        final JvmConstant jvmUtf8 = add(new JvmConstant(c.jvmConstantUtf8, "Utf8"));
        final JvmConstant jvmInteger = add(new JvmConstant(c.jvmConstantInteger, "Integer"));
        final JvmConstant jvmLong = add(new JvmConstant(c.jvmConstantLong, "Long"));
        final JvmConstant jvmFloat = add(new JvmConstant(c.jvmConstantFloat, "Float"));
        final JvmConstant jvmDouble = add(new JvmConstant(c.jvmConstantDouble, "Double"));
        final JvmConstant jvmClass = add(new JvmConstant(c.jvmConstantClass, "Class"));
        final JvmConstant jvmUnresolvedClass = add(new JvmConstant(c.jvmConstantUnresolvedClass, "UnresolvedClass"));
        final JvmConstant jvmUnresolvedClassInError = add(new JvmConstant(c.jvmConstantUnresolvedClassInError, "UnresolvedClassInError"));
        final JvmConstant jvmString = add(new JvmConstant(c.jvmConstantString, "String"));
        final JvmConstant jvmFieldref = add(new JvmConstant(c.jvmConstantFieldref, "Fieldref"));
        final JvmConstant jvmMethodref = add(new JvmConstant(c.jvmConstantMethodref, "Methodref"));
        final JvmConstant jvmInterfaceMethodref = add(new JvmConstant(c.jvmConstantInterfaceMethodref, "InterfaceMethodref"));
        final JvmConstant jvmNameAndType = add(new JvmConstant(c.jvmConstantNameAndType, "NameAndType"));
        final JvmConstant jvmMethodHandle = add(new JvmConstant(c.jvmConstantMethodHandle, "MethodHandle"));
        final JvmConstant jvmMethodHandleInError = add(new JvmConstant(c.jvmConstantMethodHandleInError, "MethodHandleInError"));
        final JvmConstant jvmMethodType = add(new JvmConstant(c.jvmConstantMethodType, "MethodType"));
        final JvmConstant jvmMethodTypeInError = add(new JvmConstant(c.jvmConstantMethodTypeInError, "MethodTypeInError"));
        final JvmConstant jvmInvokeDynamic = add(new JvmConstant(c.jvmConstantInvokeDynamic, "InvokeDynamic"));
        final JvmConstant jvmDynamic = add(new JvmConstant(c.jvmConstantDynamic, "Dynamic"));
        final JvmConstant jvmDynamicInError = add(new JvmConstant(c.jvmConstantDynamicInError, "DynamicInError"));

        private JvmConstant add(JvmConstant constant) {
            table[indexOf(constant.tag)] = constant;
            return constant;
        }

        private int indexOf(int tag) {
            if (tag >= internalMin) {
                return tag - internalMin + externalMax + 1;
            } else {
                assert tag <= externalMax;
            }
            return tag;
        }

        JvmConstant get(int tag) {
            JvmConstant res = table[indexOf(tag)];
            if (res != null) {
                return res;
            }
            throw new JVMCIError("Unknown JvmConstant tag %s", tag);
        }

        @NativeImageReinitialize private static volatile JvmConstants instance;

        static JvmConstants instance() {
            JvmConstants result = instance;
            if (result == null) {
                synchronized (JvmConstants.class) {
                    result = instance;
                    if (result == null) {
                        instance = result = new JvmConstants();
                    }
                }
            }
            return result;
        }
    }

    private static class LookupTypeCacheElement {
        int lastCpi = Integer.MIN_VALUE;
        JavaType javaType;

        LookupTypeCacheElement(int lastCpi, JavaType javaType) {
            super();
            this.lastCpi = lastCpi;
            this.javaType = javaType;
        }
    }

    /**
     * Handle to the {@code ConstantPool} VM object. The handle is in
     * {@code JVMCI::_metadata_handles}.
     */
    private final long metadataHandle;

    private volatile LookupTypeCacheElement lastLookupType;
    private final JvmConstants constants;

    /**
     * Gets the JVMCI mirror from a HotSpot constant pool.The VM is responsible for ensuring that
     * the ConstantPool is kept alive for the duration of this call and the
     * {@link HotSpotJVMCIRuntime} keeps it alive after that.
     *
     * Called from the VM.
     *
     * @param metaspaceConstantPool a metaspace ConstantPool object
     * @return the {@link HotSpotConstantPool} corresponding to {@code metaspaceConstantPool}
     */
    @SuppressWarnings("unused")
    @VMEntryPoint
    private static HotSpotConstantPool fromMetaspace(long metaspaceConstantPool) {
        return new HotSpotConstantPool(metaspaceConstantPool);
    }

    private HotSpotConstantPool(long metadataHandle) {
        this.metadataHandle = metadataHandle;
        this.constants = JvmConstants.instance();
        HandleCleaner.create(this, metadataHandle);
    }

    /**
     * Gets the holder for this constant pool as {@link HotSpotResolvedObjectTypeImpl}.
     *
     * @return holder for this constant pool
     */
    private HotSpotResolvedObjectType getHolder() {
        return compilerToVM().getResolvedJavaType(this, config().constantPoolHolderOffset, false);
    }

    /**
     * Converts a raw index from the bytecodes to a constant pool cache index by adding a
     * {@link HotSpotVMConfig#constantPoolCpCacheIndexTag constant}.
     *
     * @param rawIndex index from the bytecode
     * @param opcode bytecode to convert the index for
     * @return constant pool cache index
     */
    private static int rawIndexToConstantPoolCacheIndex(int rawIndex, int opcode) {
        int index;
        if (opcode == Bytecodes.INVOKEDYNAMIC) {
            index = rawIndex;
            // See: ConstantPool::is_invokedynamic_index
            assert index < 0 : "not an invokedynamic constant pool index " + index;
        } else {
            assert opcode == Bytecodes.GETFIELD || opcode == Bytecodes.PUTFIELD || opcode == Bytecodes.GETSTATIC || opcode == Bytecodes.PUTSTATIC || opcode == Bytecodes.INVOKEINTERFACE ||
                            opcode == Bytecodes.INVOKEVIRTUAL || opcode == Bytecodes.INVOKESPECIAL || opcode == Bytecodes.INVOKESTATIC : "unexpected invoke opcode " + opcode;
            index = rawIndex + config().constantPoolCpCacheIndexTag;
        }
        return index;
    }

    /**
     * Decode a constant pool cache index to a constant pool index.
     *
     * See {@code ConstantPool::decode_cpcache_index}.
     *
     * @param index constant pool cache index
     * @return decoded index
     */
    private static int decodeConstantPoolCacheIndex(int index) {
        if (isInvokedynamicIndex(index)) {
            return decodeInvokedynamicIndex(index);
        } else {
            return index - config().constantPoolCpCacheIndexTag;
        }
    }

    /**
     * See {@code ConstantPool::is_invokedynamic_index}.
     */
    private static boolean isInvokedynamicIndex(int index) {
        return index < 0;
    }

    /**
     * See {@code ConstantPool::decode_invokedynamic_index}.
     */
    private static int decodeInvokedynamicIndex(int i) {
        assert isInvokedynamicIndex(i) : i;
        return ~i;
    }

    long getMetaspaceConstantPool() {
        return getMetaspacePointer();
    }

    @Override
    public long getMetadataHandle() {
        return metadataHandle;
    }

    /**
     * Gets the constant pool tag at index {@code index}.
     *
     * @param index constant pool index
     * @return constant pool tag
     */
    private JvmConstant getTagAt(int index) {
        assert checkBounds(index);
        HotSpotVMConfig config = config();
        final long metaspaceConstantPoolTags = UNSAFE.getAddress(getMetaspaceConstantPool() + config.constantPoolTagsOffset);
        final int tag = UNSAFE.getByteVolatile(null, metaspaceConstantPoolTags + config.arrayU1DataOffset + index);
        if (tag == 0) {
            return null;
        }
        return constants.get(tag);
    }

    /**
     * Gets the constant pool entry at index {@code index}.
     *
     * @param index constant pool index
     * @return constant pool entry
     */
    long getEntryAt(int index) {
        assert checkBounds(index);
        int offset = index * runtime().getHostJVMCIBackend().getTarget().wordSize;
        return UNSAFE.getAddress(getMetaspaceConstantPool() + config().constantPoolSize + offset);
    }

    /**
     * Gets the integer constant pool entry at index {@code index}.
     *
     * @param index constant pool index
     * @return integer constant pool entry at index
     */
    private int getIntAt(int index) {
        assert checkTag(index, constants.jvmInteger);
        int offset = index * runtime().getHostJVMCIBackend().getTarget().wordSize;
        return UNSAFE.getInt(getMetaspaceConstantPool() + config().constantPoolSize + offset);
    }

    /**
     * Gets the long constant pool entry at index {@code index}.
     *
     * @param index constant pool index
     * @return long constant pool entry
     */
    private long getLongAt(int index) {
        assert checkTag(index, constants.jvmLong);
        int offset = index * runtime().getHostJVMCIBackend().getTarget().wordSize;
        return UNSAFE.getLong(getMetaspaceConstantPool() + config().constantPoolSize + offset);
    }

    /**
     * Gets the float constant pool entry at index {@code index}.
     *
     * @param index constant pool index
     * @return float constant pool entry
     */
    private float getFloatAt(int index) {
        assert checkTag(index, constants.jvmFloat);
        int offset = index * runtime().getHostJVMCIBackend().getTarget().wordSize;
        return UNSAFE.getFloat(getMetaspaceConstantPool() + config().constantPoolSize + offset);
    }

    /**
     * Gets the double constant pool entry at index {@code index}.
     *
     * @param index constant pool index
     * @return float constant pool entry
     */
    private double getDoubleAt(int index) {
        assert checkTag(index, constants.jvmDouble);
        int offset = index * runtime().getHostJVMCIBackend().getTarget().wordSize;
        return UNSAFE.getDouble(getMetaspaceConstantPool() + config().constantPoolSize + offset);
    }

    /**
     * Gets the {@code JVM_CONSTANT_NameAndType} constant pool entry at index {@code index}.
     *
     * @param index constant pool index
     * @return {@code JVM_CONSTANT_NameAndType} constant pool entry
     */
    private int getNameAndTypeAt(int index) {
        assert checkTag(index, constants.jvmNameAndType);
        int offset = index * runtime().getHostJVMCIBackend().getTarget().wordSize;
        return UNSAFE.getInt(getMetaspaceConstantPool() + config().constantPoolSize + offset);
    }

    /**
     * Gets the {@code JVM_CONSTANT_NameAndType} reference index constant pool entry at index
     * {@code index}.
     *
     * @param index constant pool index
     * @return {@code JVM_CONSTANT_NameAndType} reference constant pool entry
     */
    private int getNameAndTypeRefIndexAt(int index) {
        return compilerToVM().lookupNameAndTypeRefIndexInPool(this, index);
    }

    /**
     * Gets the name of a {@code JVM_CONSTANT_NameAndType} constant pool entry referenced by another
     * entry denoted by {@code which}.
     *
     * @param which constant pool index or constant pool cache index
     * @return name as {@link String}
     */
    private String getNameOf(int which) {
        return compilerToVM().lookupNameInPool(this, which);
    }

    /**
     * Gets the name reference index of a {@code JVM_CONSTANT_NameAndType} constant pool entry at
     * index {@code index}.
     *
     * @param index constant pool index
     * @return name reference index
     */
    private int getNameRefIndexAt(int index) {
        final int refIndex = getNameAndTypeAt(index);
        // name ref index is in the low 16-bits.
        return refIndex & 0xFFFF;
    }

    /**
     * Gets the signature of a {@code JVM_CONSTANT_NameAndType} constant pool entry referenced by
     * another entry denoted by {@code which}.
     *
     * @param which constant pool index or constant pool cache index
     * @return signature as {@link String}
     */
    private String getSignatureOf(int which) {
        return compilerToVM().lookupSignatureInPool(this, which);
    }

    /**
     * Gets the signature reference index of a {@code JVM_CONSTANT_NameAndType} constant pool entry
     * at index {@code index}.
     *
     * @param index constant pool index
     * @return signature reference index
     */
    private int getSignatureRefIndexAt(int index) {
        final int refIndex = getNameAndTypeAt(index);
        // signature ref index is in the high 16-bits.
        return refIndex >>> 16;
    }

    /**
     * Gets the klass reference index constant pool entry at index {@code index}.
     *
     * @param index constant pool index
     * @return klass reference index
     */
    private int getKlassRefIndexAt(int index) {
        return compilerToVM().lookupKlassRefIndexInPool(this, index);
    }

    /**
     * Gets the uncached klass reference index constant pool entry at index {@code index}. See:
     * {@code ConstantPool::uncached_klass_ref_index_at}.
     *
     * @param index constant pool index
     * @return klass reference index
     */
    private int getUncachedKlassRefIndexAt(int index) {
        assert checkTagIsFieldOrMethod(index);
        int offset = index * runtime().getHostJVMCIBackend().getTarget().wordSize;
        final int refIndex = UNSAFE.getInt(getMetaspaceConstantPool() + config().constantPoolSize + offset);
        // klass ref index is in the low 16-bits.
        return refIndex & 0xFFFF;
    }

    /**
     * Checks that the constant pool index {@code index} is in the bounds of the constant pool.
     *
     * @param index constant pool index
     * @throws AssertionError if the check fails
     */
    private boolean checkBounds(int index) {
        assert 0 <= index && index < length() : "index " + index + " not between 0 and " + length();
        return true;
    }

    /**
     * Checks that the constant pool tag at index {@code index} is equal to {@code tag}.
     *
     * @param index constant pool index
     * @param tag expected tag
     * @throws AssertionError if the check fails
     */
    private boolean checkTag(int index, JvmConstant tag) {
        final JvmConstant tagAt = getTagAt(index);
        assert tagAt == tag : "constant pool tag at index " + index + " is " + tagAt + " but expected " + tag;
        return true;
    }

    /**
     * Asserts that the constant pool tag at index {@code index} is a
     * {@link JvmConstants#jvmFieldref}, or a {@link JvmConstants#jvmMethodref}, or a
     * {@link JvmConstants#jvmInterfaceMethodref}.
     *
     * @param index constant pool index
     * @throws AssertionError if the check fails
     */
    private boolean checkTagIsFieldOrMethod(int index) {
        final JvmConstant tagAt = getTagAt(index);
        assert tagAt == constants.jvmFieldref || tagAt == constants.jvmMethodref || tagAt == constants.jvmInterfaceMethodref : tagAt;
        return true;
    }

    @Override
    public int length() {
        return UNSAFE.getInt(getMetaspaceConstantPool() + config().constantPoolLengthOffset);
    }

    public boolean hasDynamicConstant() {
        return (flags() & config().constantPoolHasDynamicConstant) != 0;
    }

    private int flags() {
        return UNSAFE.getInt(getMetaspaceConstantPool() + config().constantPoolFlagsOffset);
    }

    @Override
    public Object lookupConstant(int cpi) {
        assert cpi != 0;
        final JvmConstant tag = getTagAt(cpi);
        switch (tag.name) {
            case "Integer":
                return JavaConstant.forInt(getIntAt(cpi));
            case "Long":
                return JavaConstant.forLong(getLongAt(cpi));
            case "Float":
                return JavaConstant.forFloat(getFloatAt(cpi));
            case "Double":
                return JavaConstant.forDouble(getDoubleAt(cpi));
            case "Class":
            case "UnresolvedClass":
            case "UnresolvedClassInError":
                final int opcode = -1;  // opcode is not used
                return lookupType(cpi, opcode);
            case "String":
                /*
                 * Normally, we would expect a String here, but unsafe anonymous classes can have
                 * "pseudo strings" (arbitrary live objects) patched into a String entry. Such
                 * entries do not have a symbol in the constant pool slot.
                 */
                return compilerToVM().resolvePossiblyCachedConstantInPool(this, cpi);
            case "MethodHandle":
            case "MethodHandleInError":
            case "MethodType":
            case "MethodTypeInError":
            case "Dynamic":
            case "DynamicInError":
                return compilerToVM().resolvePossiblyCachedConstantInPool(this, cpi);
            default:
                throw new JVMCIError("Unknown constant pool tag %s", tag);
        }
    }

    @Override
    public String lookupUtf8(int cpi) {
        assert checkTag(cpi, constants.jvmUtf8);
        return compilerToVM().getSymbol(getEntryAt(cpi));
    }

    @Override
    public Signature lookupSignature(int cpi) {
        return new HotSpotSignature(runtime(), lookupUtf8(cpi));
    }

    @Override
    public JavaConstant lookupAppendix(int cpi, int opcode) {
        assert Bytecodes.isInvoke(opcode);
        final int index = rawIndexToConstantPoolCacheIndex(cpi, opcode);
        return compilerToVM().lookupAppendixInPool(this, index);
    }

    /**
     * Gets a {@link JavaType} corresponding a given resolved or unresolved type.
     *
     * @param type either a ResolvedJavaType or a String naming a unresolved type.
     */
    private static JavaType getJavaType(final Object type) {
        if (type instanceof String) {
            String name = (String) type;
            return UnresolvedJavaType.create("L" + name + ";");
        } else {
            return (JavaType) type;
        }
    }

    @Override
    public JavaMethod lookupMethod(int cpi, int opcode) {
        final int index = rawIndexToConstantPoolCacheIndex(cpi, opcode);
        final HotSpotResolvedJavaMethod method = compilerToVM().lookupMethodInPool(this, index, (byte) opcode);
        if (method != null) {
            return method;
        } else {
            // Get the method's name and signature.
            String name = getNameOf(index);
            HotSpotSignature signature = new HotSpotSignature(runtime(), getSignatureOf(index));
            if (opcode == Bytecodes.INVOKEDYNAMIC) {
                HotSpotResolvedObjectType holder = runtime().getMethodHandleClass();
                return new UnresolvedJavaMethod(name, signature, holder);
            } else {
                final int klassIndex = getKlassRefIndexAt(index);
                final Object type = compilerToVM().lookupKlassInPool(this, klassIndex);
                JavaType holder = getJavaType(type);
                return new UnresolvedJavaMethod(name, signature, holder);
            }
        }
    }

    @Override
    public JavaType lookupType(int cpi, int opcode) {
        final LookupTypeCacheElement elem = this.lastLookupType;
        if (elem != null && elem.lastCpi == cpi) {
            return elem.javaType;
        } else {
            final Object type = compilerToVM().lookupKlassInPool(this, cpi);
            JavaType result = getJavaType(type);
            if (result instanceof ResolvedJavaType) {
                this.lastLookupType = new LookupTypeCacheElement(cpi, result);
            }
            return result;
        }
    }

    @Override
    public JavaType lookupReferencedType(int cpi, int opcode) {
        int index;
        switch (opcode) {
            case Bytecodes.CHECKCAST:
            case Bytecodes.INSTANCEOF:
            case Bytecodes.NEW:
            case Bytecodes.ANEWARRAY:
            case Bytecodes.MULTIANEWARRAY:
            case Bytecodes.LDC:
            case Bytecodes.LDC_W:
            case Bytecodes.LDC2_W:
                index = cpi;
                break;
            case Bytecodes.GETSTATIC:
            case Bytecodes.PUTSTATIC:
            case Bytecodes.GETFIELD:
            case Bytecodes.PUTFIELD:
            case Bytecodes.INVOKEVIRTUAL:
            case Bytecodes.INVOKESPECIAL:
            case Bytecodes.INVOKESTATIC:
            case Bytecodes.INVOKEINTERFACE: {
                index = rawIndexToConstantPoolCacheIndex(cpi, opcode);
                index = getKlassRefIndexAt(index);
                break;
            }
            default:
                throw JVMCIError.shouldNotReachHere("Unexpected opcode " + opcode);
        }
        final Object type = compilerToVM().lookupKlassInPool(this, index);
        return getJavaType(type);
    }

    @Override
    public JavaField lookupField(int cpi, ResolvedJavaMethod method, int opcode) {
        final int index = rawIndexToConstantPoolCacheIndex(cpi, opcode);
        final int nameAndTypeIndex = getNameAndTypeRefIndexAt(index);
        final int typeIndex = getSignatureRefIndexAt(nameAndTypeIndex);
        String typeName = lookupUtf8(typeIndex);
        JavaType type = runtime().lookupType(typeName, getHolder(), false);

        final int holderIndex = getKlassRefIndexAt(index);
        JavaType holder = lookupType(holderIndex, opcode);

        if (holder instanceof HotSpotResolvedObjectTypeImpl) {
            int[] info = new int[3];
            HotSpotResolvedObjectTypeImpl resolvedHolder;
            try {
                resolvedHolder = compilerToVM().resolveFieldInPool(this, index, (HotSpotResolvedJavaMethodImpl) method, (byte) opcode, info);
            } catch (Throwable t) {
                /*
                 * If there was an exception resolving the field we give up and return an unresolved
                 * field.
                 */
                return new UnresolvedJavaField(holder, lookupUtf8(getNameRefIndexAt(nameAndTypeIndex)), type);
            }
            final int flags = info[0];
            final int offset = info[1];
            final int fieldIndex = info[2];
            HotSpotResolvedJavaField result = resolvedHolder.createField(type, offset, flags, fieldIndex);
            return result;
        } else {
            return new UnresolvedJavaField(holder, lookupUtf8(getNameRefIndexAt(nameAndTypeIndex)), type);
        }
    }

    /**
     * Converts a raw index from the bytecodes to a constant pool index (not a cache index).
     *
     * @param rawIndex index from the bytecode
     *
     * @param opcode bytecode to convert the index for
     *
     * @return constant pool index
     */
    public int rawIndexToConstantPoolIndex(int rawIndex, int opcode) {
        int index;
        if (isInvokedynamicIndex(rawIndex)) {
            assert opcode == Bytecodes.INVOKEDYNAMIC;
            index = decodeInvokedynamicIndex(rawIndex) + config().constantPoolCpCacheIndexTag;
        } else {
            assert opcode != Bytecodes.INVOKEDYNAMIC;
            index = rawIndexToConstantPoolCacheIndex(rawIndex, opcode);
        }
        return compilerToVM().constantPoolRemapInstructionOperandFromCache(this, index);
    }

    @Override
    public void loadReferencedType(int cpi, int opcode) {
        loadReferencedType(cpi, opcode, true /* initialize */);
    }

    @SuppressWarnings("fallthrough")
    public void loadReferencedType(int cpi, int opcode, boolean initialize) {
        int index;
        switch (opcode) {
            case Bytecodes.CHECKCAST:
            case Bytecodes.INSTANCEOF:
            case Bytecodes.NEW:
            case Bytecodes.ANEWARRAY:
            case Bytecodes.MULTIANEWARRAY:
            case Bytecodes.LDC:
            case Bytecodes.LDC_W:
            case Bytecodes.LDC2_W:
                index = cpi;
                break;
            case Bytecodes.INVOKEDYNAMIC: {
                // invokedynamic instructions point to a constant pool cache entry.
                index = decodeConstantPoolCacheIndex(cpi) + config().constantPoolCpCacheIndexTag;
                index = compilerToVM().constantPoolRemapInstructionOperandFromCache(this, index);
                break;
            }
            case Bytecodes.GETSTATIC:
            case Bytecodes.PUTSTATIC:
            case Bytecodes.GETFIELD:
            case Bytecodes.PUTFIELD:
            case Bytecodes.INVOKEVIRTUAL:
            case Bytecodes.INVOKESPECIAL:
            case Bytecodes.INVOKESTATIC:
            case Bytecodes.INVOKEINTERFACE: {
                // invoke and field instructions point to a constant pool cache entry.
                index = rawIndexToConstantPoolCacheIndex(cpi, opcode);
                index = compilerToVM().constantPoolRemapInstructionOperandFromCache(this, index);
                break;
            }
            default:
                throw JVMCIError.shouldNotReachHere("Unexpected opcode " + opcode);
        }

        final JvmConstant tag = getTagAt(index);
        if (tag == null) {
            assert getTagAt(index - 1) == constants.jvmDouble || getTagAt(index - 1) == constants.jvmLong;
            return;
        }
        switch (tag.name) {
            case "Methodref":
            case "Fieldref":
            case "InterfaceMethodref":
                index = getUncachedKlassRefIndexAt(index);
                // Read the tag only once because it could change between multiple reads.
                final JvmConstant klassTag = getTagAt(index);
                assert klassTag == constants.jvmClass || klassTag == constants.jvmUnresolvedClass || klassTag == constants.jvmUnresolvedClassInError : klassTag;
                // fall through
            case "Class":
            case "UnresolvedClass":
            case "UnresolvedClassInError":
                final HotSpotResolvedObjectTypeImpl type = compilerToVM().resolveTypeInPool(this, index);
                if (initialize && !type.isPrimitive() && !type.isArray()) {
                    type.ensureInitialized();
                }
                if (tag == constants.jvmMethodref) {
                    if (Bytecodes.isInvokeHandleAlias(opcode) && isSignaturePolymorphicHolder(type)) {
                        final int methodRefCacheIndex = rawIndexToConstantPoolCacheIndex(cpi, opcode);
                        assert checkTag(compilerToVM().constantPoolRemapInstructionOperandFromCache(this, methodRefCacheIndex), constants.jvmMethodref);
                        compilerToVM().resolveInvokeHandleInPool(this, methodRefCacheIndex);
                    }
                }

                break;
            case "InvokeDynamic":
                if (isInvokedynamicIndex(cpi)) {
                    compilerToVM().resolveInvokeDynamicInPool(this, cpi);
                }
                break;
            default:
                // nothing
                break;
        }

    }

    // Lazily initialized.
    private static String[] signaturePolymorphicHolders;

    /**
     * Determines if {@code type} contains signature polymorphic methods.
     */
    @SuppressFBWarnings(value = "LI_LAZY_INIT_STATIC", justification = "signaturePolymorphicHolders is a cache, not a singleton that must be constructed exactly once" +
                    "and compiler re-ordering is not an issue due to the VM call")
    static boolean isSignaturePolymorphicHolder(final ResolvedJavaType type) {
        String name = type.getName();
        if (signaturePolymorphicHolders == null) {
            signaturePolymorphicHolders = compilerToVM().getSignaturePolymorphicHolders();
        }
        for (String holder : signaturePolymorphicHolders) {
            if (name.equals(holder)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for a resolved dynamic adapter method at the specified index, resulting from either a
     * resolved invokedynamic or invokevirtual on a signature polymorphic MethodHandle method
     * (HotSpot invokehandle).
     *
     * @param cpi the constant pool index
     * @param opcode the opcode of the instruction for which the lookup is being performed
     * @return {@code true} if a signature polymorphic method reference was found, otherwise
     *         {@code false}
     */
    public boolean isResolvedDynamicInvoke(int cpi, int opcode) {
        if (Bytecodes.isInvokeHandleAlias(opcode)) {
            final int methodRefCacheIndex = rawIndexToConstantPoolCacheIndex(cpi, opcode);
            assert checkTag(compilerToVM().constantPoolRemapInstructionOperandFromCache(this, methodRefCacheIndex), constants.jvmMethodref);
            int op = compilerToVM().isResolvedInvokeHandleInPool(this, methodRefCacheIndex);
            return op == opcode;
        }
        return false;
    }

    public String getSourceFileName() {
        final int sourceFileNameIndex = UNSAFE.getChar(getMetaspaceConstantPool() + config().constantPoolSourceFileNameIndexOffset);
        if (sourceFileNameIndex == 0) {
            return null;
        }
        return lookupUtf8(sourceFileNameIndex);
    }

    @Override
    public String toString() {
        HotSpotResolvedObjectType holder = getHolder();
        return "HotSpotConstantPool<" + holder.toJavaName() + ">";
    }
}
