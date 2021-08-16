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

import static jdk.vm.ci.hotspot.HotSpotJVMCIRuntime.runtime;
import static jdk.vm.ci.hotspot.UnsafeAccess.UNSAFE;

import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.services.Services;
import jdk.internal.misc.Unsafe;

/**
 * Used to access native configuration details.
 *
 * All non-static, public fields in this class are so that they can be compiled as constants.
 */
class HotSpotVMConfig extends HotSpotVMConfigAccess {

    /**
     * Gets the configuration associated with the singleton {@link HotSpotJVMCIRuntime}.
     */
    static HotSpotVMConfig config() {
        return runtime().getConfig();
    }

    private final String osArch = getHostArchitectureName();

    HotSpotVMConfig(HotSpotVMConfigStore store) {
        super(store);

        int speculationLengthBits = getConstant("JVMCINMethodData::SPECULATION_LENGTH_BITS", Integer.class);
        JVMCIError.guarantee(HotSpotSpeculationEncoding.LENGTH_BITS == speculationLengthBits, "%d != %d", HotSpotSpeculationEncoding.LENGTH_BITS, speculationLengthBits);
    }

    /**
     * Gets the host architecture name for the purpose of finding the corresponding
     * {@linkplain HotSpotJVMCIBackendFactory backend}.
     */
    String getHostArchitectureName() {
        String arch = Services.getSavedProperty("os.arch");
        switch (arch) {
            case "x86_64":
                return "amd64";

            default:
                return arch;
        }
    }

    final boolean useDeferredInitBarriers = getFlag("ReduceInitialCardMarks", Boolean.class);

    final boolean useCompressedOops = getFlag("UseCompressedOops", Boolean.class);

    final int objectAlignment = getFlag("ObjectAlignmentInBytes", Integer.class);

    final int hubOffset = getFieldOffset("oopDesc::_metadata._klass", Integer.class, "Klass*");

    final int subklassOffset = getFieldOffset("Klass::_subklass", Integer.class, "Klass*");
    final int superOffset = getFieldOffset("Klass::_super", Integer.class, "Klass*");
    final int nextSiblingOffset = getFieldOffset("Klass::_next_sibling", Integer.class, "Klass*");
    final int superCheckOffsetOffset = getFieldOffset("Klass::_super_check_offset", Integer.class, "juint");
    final int secondarySuperCacheOffset = getFieldOffset("Klass::_secondary_super_cache", Integer.class, "Klass*");

    final int classLoaderDataOffset = getFieldOffset("Klass::_class_loader_data", Integer.class, "ClassLoaderData*");

    /**
     * The offset of the _java_mirror field (of type {@link Class}) in a Klass.
     */
    final int javaMirrorOffset = getFieldOffset("Klass::_java_mirror", Integer.class, "OopHandle");

    final int klassAccessFlagsOffset = getFieldOffset("Klass::_access_flags", Integer.class, "AccessFlags");
    final int klassLayoutHelperOffset = getFieldOffset("Klass::_layout_helper", Integer.class, "jint");

    final int klassLayoutHelperNeutralValue = getConstant("Klass::_lh_neutral_value", Integer.class);
    final int klassLayoutHelperInstanceSlowPathBit = getConstant("Klass::_lh_instance_slow_path_bit", Integer.class);

    final int vtableEntrySize = getFieldValue("CompilerToVM::Data::sizeof_vtableEntry", Integer.class, "int");
    final int vtableEntryMethodOffset = getFieldOffset("vtableEntry::_method", Integer.class, "Method*");

    final int instanceKlassInitStateOffset = getFieldOffset("InstanceKlass::_init_state", Integer.class, "u1");
    final int instanceKlassConstantsOffset = getFieldOffset("InstanceKlass::_constants", Integer.class, "ConstantPool*");
    final int instanceKlassFieldsOffset = getFieldOffset("InstanceKlass::_fields", Integer.class, "Array<u2>*");
    final int instanceKlassAnnotationsOffset = getFieldOffset("InstanceKlass::_annotations", Integer.class, "Annotations*");
    final int instanceKlassMiscFlagsOffset = getFieldOffset("InstanceKlass::_misc_flags", Integer.class, "u2");
    final int klassVtableStartOffset = getFieldValue("CompilerToVM::Data::Klass_vtable_start_offset", Integer.class, "int");
    final int klassVtableLengthOffset = getFieldValue("CompilerToVM::Data::Klass_vtable_length_offset", Integer.class, "int");

    final int instanceKlassStateLinked = getConstant("InstanceKlass::linked", Integer.class);
    final int instanceKlassStateFullyInitialized = getConstant("InstanceKlass::fully_initialized", Integer.class);
    final int instanceKlassStateBeingInitialized = getConstant("InstanceKlass::being_initialized", Integer.class);

    final int annotationsFieldAnnotationsOffset = getFieldOffset("Annotations::_fields_annotations", Integer.class, "Array<AnnotationArray*>*");
    final int fieldsAnnotationsBaseOffset = getFieldValue("CompilerToVM::Data::_fields_annotations_base_offset", Integer.class, "int");

    final int arrayU1LengthOffset = getFieldOffset("Array<int>::_length", Integer.class, "int");
    final int arrayU1DataOffset = getFieldOffset("Array<u1>::_data", Integer.class);
    final int arrayU2DataOffset = getFieldOffset("Array<u2>::_data", Integer.class);

    final int fieldInfoAccessFlagsOffset = getConstant("FieldInfo::access_flags_offset", Integer.class);
    final int fieldInfoNameIndexOffset = getConstant("FieldInfo::name_index_offset", Integer.class);
    final int fieldInfoSignatureIndexOffset = getConstant("FieldInfo::signature_index_offset", Integer.class);
    final int fieldInfoLowPackedOffset = getConstant("FieldInfo::low_packed_offset", Integer.class);
    final int fieldInfoHighPackedOffset = getConstant("FieldInfo::high_packed_offset", Integer.class);
    final int fieldInfoFieldSlots = getConstant("FieldInfo::field_slots", Integer.class);

    final int fieldInfoTagSize = getConstant("FIELDINFO_TAG_SIZE", Integer.class);

    final int jvmAccHasFinalizer = getConstant("JVM_ACC_HAS_FINALIZER", Integer.class);
    final int jvmAccFieldInternal = getConstant("JVM_ACC_FIELD_INTERNAL", Integer.class);
    final int jvmAccFieldStable = getConstant("JVM_ACC_FIELD_STABLE", Integer.class);
    final int jvmAccFieldHasGenericSignature = getConstant("JVM_ACC_FIELD_HAS_GENERIC_SIGNATURE", Integer.class);
    final int jvmAccIsCloneableFast = getConstant("JVM_ACC_IS_CLONEABLE_FAST", Integer.class);

    // These modifiers are not public in Modifier so we get them via vmStructs.
    final int jvmAccSynthetic = getConstant("JVM_ACC_SYNTHETIC", Integer.class);
    final int jvmAccAnnotation = getConstant("JVM_ACC_ANNOTATION", Integer.class);
    final int jvmAccBridge = getConstant("JVM_ACC_BRIDGE", Integer.class);
    final int jvmAccVarargs = getConstant("JVM_ACC_VARARGS", Integer.class);
    final int jvmAccEnum = getConstant("JVM_ACC_ENUM", Integer.class);
    final int jvmAccInterface = getConstant("JVM_ACC_INTERFACE", Integer.class);

    final int jvmMiscFlagsHasDefaultMethods = getConstant("InstanceKlass::_misc_has_nonstatic_concrete_methods", Integer.class);
    final int jvmMiscFlagsDeclaresDefaultMethods = getConstant("InstanceKlass::_misc_declares_nonstatic_concrete_methods", Integer.class);

    // This is only valid on AMD64.
    final int runtimeCallStackSize = getConstant("frame::arg_reg_save_area_bytes", Integer.class, osArch.equals("amd64") ? null : 0);

    private final int markWordNoHashInPlace = getConstant("markWord::no_hash_in_place", Integer.class);
    private final int markWordNoLockInPlace = getConstant("markWord::no_lock_in_place", Integer.class);

    /**
     * See {@code markWord::prototype()}.
     */
    long prototypeMarkWord() {
        return markWordNoHashInPlace | markWordNoLockInPlace;
    }

    final int methodAccessFlagsOffset = getFieldOffset("Method::_access_flags", Integer.class, "AccessFlags");
    final int methodConstMethodOffset = getFieldOffset("Method::_constMethod", Integer.class, "ConstMethod*");
    final int methodIntrinsicIdOffset = getFieldOffset("Method::_intrinsic_id", Integer.class, "u2");
    final int methodFlagsOffset = getFieldOffset("Method::_flags", Integer.class, "u2");
    final int methodVtableIndexOffset = getFieldOffset("Method::_vtable_index", Integer.class, "int");

    final int methodDataOffset = getFieldOffset("Method::_method_data", Integer.class, "MethodData*");
    final int methodCodeOffset = getFieldOffset("Method::_code", Integer.class, "CompiledMethod*");

    final int methodFlagsCallerSensitive = getConstant("Method::_caller_sensitive", Integer.class);
    final int methodFlagsForceInline = getConstant("Method::_force_inline", Integer.class);
    final int methodFlagsIntrinsicCandidate = getConstant("Method::_intrinsic_candidate", Integer.class);
    final int methodFlagsDontInline = getConstant("Method::_dont_inline", Integer.class);
    final int methodFlagsReservedStackAccess = getConstant("Method::_reserved_stack_access", Integer.class);
    final int nonvirtualVtableIndex = getConstant("Method::nonvirtual_vtable_index", Integer.class);
    final int invalidVtableIndex = getConstant("Method::invalid_vtable_index", Integer.class);

    final int methodDataSize = getFieldOffset("MethodData::_size", Integer.class, "int");
    final int methodDataDataSize = getFieldOffset("MethodData::_data_size", Integer.class, "int");
    final int methodDataOopDataOffset = getFieldOffset("MethodData::_data[0]", Integer.class, "intptr_t");
    final int methodDataOopTrapHistoryOffset = getFieldOffset("MethodData::_compiler_counters._trap_hist._array[0]", Integer.class, "u1");
    final int methodDataIRSizeOffset = getFieldOffset("MethodData::_jvmci_ir_size", Integer.class, "int");

    final int methodDataDecompiles = getFieldOffset("MethodData::_compiler_counters._nof_decompiles", Integer.class, "uint");
    final int methodDataOverflowRecompiles = getFieldOffset("MethodData::_compiler_counters._nof_overflow_recompiles", Integer.class, "uint");
    final int methodDataOverflowTraps = getFieldOffset("MethodData::_compiler_counters._nof_overflow_traps", Integer.class, "uint");

    final int nmethodCompLevelOffset = getFieldOffset("nmethod::_comp_level", Integer.class, "int");

    final int compilationLevelNone = getConstant("CompLevel_none", Integer.class);
    final int compilationLevelSimple = getConstant("CompLevel_simple", Integer.class);
    final int compilationLevelLimitedProfile = getConstant("CompLevel_limited_profile", Integer.class);
    final int compilationLevelFullProfile = getConstant("CompLevel_full_profile", Integer.class);
    final int compilationLevelFullOptimization = getConstant("CompLevel_full_optimization", Integer.class);

    final int compLevelAdjustmentNone = getConstant("JVMCIRuntime::none", Integer.class);
    final int compLevelAdjustmentByHolder = getConstant("JVMCIRuntime::by_holder", Integer.class);
    final int compLevelAdjustmentByFullSignature = getConstant("JVMCIRuntime::by_full_signature", Integer.class);

    final int invocationEntryBci = getConstant("InvocationEntryBci", Integer.class);

    final int extraStackEntries = getFieldValue("CompilerToVM::Data::Method_extra_stack_entries", Integer.class, "int");

    final int constMethodConstantsOffset = getFieldOffset("ConstMethod::_constants", Integer.class, "ConstantPool*");
    final int constMethodFlagsOffset = getFieldOffset("ConstMethod::_flags", Integer.class, "u2");
    final int constMethodCodeSizeOffset = getFieldOffset("ConstMethod::_code_size", Integer.class, "u2");
    final int constMethodNameIndexOffset = getFieldOffset("ConstMethod::_name_index", Integer.class, "u2");
    final int constMethodSignatureIndexOffset = getFieldOffset("ConstMethod::_signature_index", Integer.class, "u2");
    final int constMethodMethodIdnumOffset = getFieldOffset("ConstMethod::_method_idnum", Integer.class, "u2");
    final int constMethodMaxStackOffset = getFieldOffset("ConstMethod::_max_stack", Integer.class, "u2");
    final int methodMaxLocalsOffset = getFieldOffset("ConstMethod::_max_locals", Integer.class, "u2");

    final int constMethodHasLineNumberTable = getConstant("ConstMethod::_has_linenumber_table", Integer.class);
    final int constMethodHasLocalVariableTable = getConstant("ConstMethod::_has_localvariable_table", Integer.class);
    final int constMethodHasMethodAnnotations = getConstant("ConstMethod::_has_method_annotations", Integer.class);
    final int constMethodHasParameterAnnotations = getConstant("ConstMethod::_has_parameter_annotations", Integer.class);
    final int constMethodHasExceptionTable = getConstant("ConstMethod::_has_exception_table", Integer.class);

    final int exceptionTableElementSize = getFieldValue("CompilerToVM::Data::sizeof_ExceptionTableElement", Integer.class, "int");
    final int exceptionTableElementStartPcOffset = getFieldOffset("ExceptionTableElement::start_pc", Integer.class, "u2");
    final int exceptionTableElementEndPcOffset = getFieldOffset("ExceptionTableElement::end_pc", Integer.class, "u2");
    final int exceptionTableElementHandlerPcOffset = getFieldOffset("ExceptionTableElement::handler_pc", Integer.class, "u2");
    final int exceptionTableElementCatchTypeIndexOffset = getFieldOffset("ExceptionTableElement::catch_type_index", Integer.class, "u2");

    final int localVariableTableElementSize = getFieldValue("CompilerToVM::Data::sizeof_LocalVariableTableElement", Integer.class, "int");
    final int localVariableTableElementStartBciOffset = getFieldOffset("LocalVariableTableElement::start_bci", Integer.class, "u2");
    final int localVariableTableElementLengthOffset = getFieldOffset("LocalVariableTableElement::length", Integer.class, "u2");
    final int localVariableTableElementNameCpIndexOffset = getFieldOffset("LocalVariableTableElement::name_cp_index", Integer.class, "u2");
    final int localVariableTableElementDescriptorCpIndexOffset = getFieldOffset("LocalVariableTableElement::descriptor_cp_index", Integer.class, "u2");
    final int localVariableTableElementSlotOffset = getFieldOffset("LocalVariableTableElement::slot", Integer.class, "u2");

    final int constantPoolSize = getFieldValue("CompilerToVM::Data::sizeof_ConstantPool", Integer.class, "int");
    final int constantPoolTagsOffset = getFieldOffset("ConstantPool::_tags", Integer.class, "Array<u1>*");
    final int constantPoolHolderOffset = getFieldOffset("ConstantPool::_pool_holder", Integer.class, "InstanceKlass*");
    final int constantPoolLengthOffset = getFieldOffset("ConstantPool::_length", Integer.class, "int");
    final int constantPoolFlagsOffset = getFieldOffset("ConstantPool::_flags", Integer.class, "u2");

    final int constantPoolCpCacheIndexTag = getConstant("ConstantPool::CPCACHE_INDEX_TAG", Integer.class);
    final int constantPoolHasDynamicConstant = getConstant("ConstantPool::_has_dynamic_constant", Integer.class);
    final int constantPoolSourceFileNameIndexOffset = getFieldOffset("ConstantPool::_source_file_name_index", Integer.class, "u2");

    final int jvmConstantUtf8 = getConstant("JVM_CONSTANT_Utf8", Integer.class);
    final int jvmConstantInteger = getConstant("JVM_CONSTANT_Integer", Integer.class);
    final int jvmConstantLong = getConstant("JVM_CONSTANT_Long", Integer.class);
    final int jvmConstantFloat = getConstant("JVM_CONSTANT_Float", Integer.class);
    final int jvmConstantDouble = getConstant("JVM_CONSTANT_Double", Integer.class);
    final int jvmConstantClass = getConstant("JVM_CONSTANT_Class", Integer.class);
    final int jvmConstantUnresolvedClass = getConstant("JVM_CONSTANT_UnresolvedClass", Integer.class);
    final int jvmConstantUnresolvedClassInError = getConstant("JVM_CONSTANT_UnresolvedClassInError", Integer.class);
    final int jvmConstantString = getConstant("JVM_CONSTANT_String", Integer.class);
    final int jvmConstantFieldref = getConstant("JVM_CONSTANT_Fieldref", Integer.class);
    final int jvmConstantMethodref = getConstant("JVM_CONSTANT_Methodref", Integer.class);
    final int jvmConstantInterfaceMethodref = getConstant("JVM_CONSTANT_InterfaceMethodref", Integer.class);
    final int jvmConstantNameAndType = getConstant("JVM_CONSTANT_NameAndType", Integer.class);
    final int jvmConstantMethodHandle = getConstant("JVM_CONSTANT_MethodHandle", Integer.class);
    final int jvmConstantMethodHandleInError = getConstant("JVM_CONSTANT_MethodHandleInError", Integer.class);
    final int jvmConstantMethodType = getConstant("JVM_CONSTANT_MethodType", Integer.class);
    final int jvmConstantMethodTypeInError = getConstant("JVM_CONSTANT_MethodTypeInError", Integer.class);
    final int jvmConstantDynamic = getConstant("JVM_CONSTANT_Dynamic", Integer.class);
    final int jvmConstantDynamicInError = getConstant("JVM_CONSTANT_DynamicInError", Integer.class);
    final int jvmConstantInvokeDynamic = getConstant("JVM_CONSTANT_InvokeDynamic", Integer.class);

    final int jvmConstantExternalMax = getConstant("JVM_CONSTANT_ExternalMax", Integer.class);
    final int jvmConstantInternalMin = getConstant("JVM_CONSTANT_InternalMin", Integer.class);
    final int jvmConstantInternalMax = getConstant("JVM_CONSTANT_InternalMax", Integer.class);

    final int heapWordSize = getConstant("HeapWordSize", Integer.class);

    final long symbolVmSymbols = getFieldAddress("Symbol::_vm_symbols[0]", "Symbol*");
    final int vmSymbolsFirstSID = getConstant("vmSymbols::FIRST_SID", Integer.class);
    final int vmSymbolsSIDLimit = getConstant("vmSymbols::SID_LIMIT", Integer.class);

    final long symbolInit = getFieldValue("CompilerToVM::Data::symbol_init", Long.class);
    final long symbolClinit = getFieldValue("CompilerToVM::Data::symbol_clinit", Long.class);

    /**
     * Returns the symbol in the {@code vmSymbols} table at position {@code index} as a
     * {@link String}.
     *
     * @param index position in the symbol table
     * @return the symbol at position id
     */
    String symbolAt(int index) {
        HotSpotJVMCIRuntime runtime = runtime();
        assert vmSymbolsFirstSID <= index && index < vmSymbolsSIDLimit : "index " + index + " is out of bounds";
        int offset = index * Unsafe.ADDRESS_SIZE;
        return runtime.getCompilerToVM().getSymbol(UNSAFE.getAddress(symbolVmSymbols + offset));
    }

    final int universeBaseVtableSize = getFieldValue("CompilerToVM::Data::Universe_base_vtable_size", Integer.class, "int");

    final int baseVtableLength() {
        return universeBaseVtableSize / (vtableEntrySize / heapWordSize);
    }

    final int klassOffset = getFieldValue("java_lang_Class::_klass_offset", Integer.class, "int");

    /**
     * The DataLayout header size is the same as the cell size.
     */
    final int dataLayoutHeaderSize = getConstant("DataLayout::cell_size", Integer.class);
    final int dataLayoutTagOffset = getFieldOffset("DataLayout::_header._struct._tag", Integer.class, "u1");
    final int dataLayoutFlagsOffset = getFieldOffset("DataLayout::_header._struct._flags", Integer.class, "u1");
    final int dataLayoutBCIOffset = getFieldOffset("DataLayout::_header._struct._bci", Integer.class, "u2");
    final int dataLayoutCellSize = getConstant("DataLayout::cell_size", Integer.class);

    final int dataLayoutNoTag = getConstant("DataLayout::no_tag", Integer.class);
    final int dataLayoutBitDataTag = getConstant("DataLayout::bit_data_tag", Integer.class);
    final int dataLayoutCounterDataTag = getConstant("DataLayout::counter_data_tag", Integer.class);
    final int dataLayoutJumpDataTag = getConstant("DataLayout::jump_data_tag", Integer.class);
    final int dataLayoutReceiverTypeDataTag = getConstant("DataLayout::receiver_type_data_tag", Integer.class);
    final int dataLayoutVirtualCallDataTag = getConstant("DataLayout::virtual_call_data_tag", Integer.class);
    final int dataLayoutRetDataTag = getConstant("DataLayout::ret_data_tag", Integer.class);
    final int dataLayoutBranchDataTag = getConstant("DataLayout::branch_data_tag", Integer.class);
    final int dataLayoutMultiBranchDataTag = getConstant("DataLayout::multi_branch_data_tag", Integer.class);
    final int dataLayoutArgInfoDataTag = getConstant("DataLayout::arg_info_data_tag", Integer.class);
    final int dataLayoutCallTypeDataTag = getConstant("DataLayout::call_type_data_tag", Integer.class);
    final int dataLayoutVirtualCallTypeDataTag = getConstant("DataLayout::virtual_call_type_data_tag", Integer.class);
    final int dataLayoutParametersTypeDataTag = getConstant("DataLayout::parameters_type_data_tag", Integer.class);
    final int dataLayoutSpeculativeTrapDataTag = getConstant("DataLayout::speculative_trap_data_tag", Integer.class);

    final int bciProfileWidth = getFlag("BciProfileWidth", Integer.class);
    final int typeProfileWidth = getFlag("TypeProfileWidth", Integer.class);
    final int methodProfileWidth = getFlag("MethodProfileWidth", Integer.class, 0);

    final int deoptReasonNone = getConstant("Deoptimization::Reason_none", Integer.class);
    final int deoptReasonNullCheck = getConstant("Deoptimization::Reason_null_check", Integer.class);
    final int deoptReasonRangeCheck = getConstant("Deoptimization::Reason_range_check", Integer.class);
    final int deoptReasonClassCheck = getConstant("Deoptimization::Reason_class_check", Integer.class);
    final int deoptReasonArrayCheck = getConstant("Deoptimization::Reason_array_check", Integer.class);
    final int deoptReasonUnreached0 = getConstant("Deoptimization::Reason_unreached0", Integer.class);
    final int deoptReasonTypeCheckInlining = getConstant("Deoptimization::Reason_type_checked_inlining", Integer.class);
    final int deoptReasonOptimizedTypeCheck = getConstant("Deoptimization::Reason_optimized_type_check", Integer.class);
    final int deoptReasonNotCompiledExceptionHandler = getConstant("Deoptimization::Reason_not_compiled_exception_handler", Integer.class);
    final int deoptReasonUnresolved = getConstant("Deoptimization::Reason_unresolved", Integer.class);
    final int deoptReasonJsrMismatch = getConstant("Deoptimization::Reason_jsr_mismatch", Integer.class);
    final int deoptReasonDiv0Check = getConstant("Deoptimization::Reason_div0_check", Integer.class);
    final int deoptReasonConstraint = getConstant("Deoptimization::Reason_constraint", Integer.class);
    final int deoptReasonLoopLimitCheck = getConstant("Deoptimization::Reason_loop_limit_check", Integer.class);
    final int deoptReasonAliasing = getConstant("Deoptimization::Reason_aliasing", Integer.class);
    final int deoptReasonTransferToInterpreter = getConstant("Deoptimization::Reason_transfer_to_interpreter", Integer.class);
    final int deoptReasonOSROffset = getConstant("Deoptimization::Reason_LIMIT", Integer.class);

    final int deoptActionNone = getConstant("Deoptimization::Action_none", Integer.class);
    final int deoptActionMaybeRecompile = getConstant("Deoptimization::Action_maybe_recompile", Integer.class);
    final int deoptActionReinterpret = getConstant("Deoptimization::Action_reinterpret", Integer.class);
    final int deoptActionMakeNotEntrant = getConstant("Deoptimization::Action_make_not_entrant", Integer.class);
    final int deoptActionMakeNotCompilable = getConstant("Deoptimization::Action_make_not_compilable", Integer.class);

    final int deoptimizationActionBits = getConstant("Deoptimization::_action_bits", Integer.class);
    final int deoptimizationReasonBits = getConstant("Deoptimization::_reason_bits", Integer.class);
    final int deoptimizationDebugIdBits = getConstant("Deoptimization::_debug_id_bits", Integer.class);
    final int deoptimizationActionShift = getConstant("Deoptimization::_action_shift", Integer.class);
    final int deoptimizationReasonShift = getConstant("Deoptimization::_reason_shift", Integer.class);
    final int deoptimizationDebugIdShift = getConstant("Deoptimization::_debug_id_shift", Integer.class);

    final int vmIntrinsicInvokeBasic = getConstant("vmIntrinsics::_invokeBasic", Integer.class);
    final int vmIntrinsicLinkToVirtual = getConstant("vmIntrinsics::_linkToVirtual", Integer.class);
    final int vmIntrinsicLinkToStatic = getConstant("vmIntrinsics::_linkToStatic", Integer.class);
    final int vmIntrinsicLinkToSpecial = getConstant("vmIntrinsics::_linkToSpecial", Integer.class);
    final int vmIntrinsicLinkToInterface = getConstant("vmIntrinsics::_linkToInterface", Integer.class);

    final int codeInstallResultOk = getConstant("JVMCI::ok", Integer.class);
    final int codeInstallResultDependenciesFailed = getConstant("JVMCI::dependencies_failed", Integer.class);
    final int codeInstallResultCacheFull = getConstant("JVMCI::cache_full", Integer.class);
    final int codeInstallResultCodeTooLarge = getConstant("JVMCI::code_too_large", Integer.class);
    final int codeInstallResultNMethodReclaimed = getConstant("JVMCI::nmethod_reclaimed", Integer.class);
    final int codeInstallResultFirstPermanentBailout = getConstant("JVMCI::first_permanent_bailout", Integer.class);

    String getCodeInstallResultDescription(int codeInstallResult) {
        if (codeInstallResult == codeInstallResultOk) {
            return "ok";
        }
        if (codeInstallResult == codeInstallResultDependenciesFailed) {
            return "dependencies failed";
        }
        if (codeInstallResult == codeInstallResultCacheFull) {
            return "code cache is full";
        }
        if (codeInstallResult == codeInstallResultCodeTooLarge) {
            return "code is too large";
        }
        if (codeInstallResult == codeInstallResultNMethodReclaimed) {
            return "nmethod reclaimed";
        }
        assert false : codeInstallResult;
        return "unknown";
    }

    final int bitDataExceptionSeenFlag = getConstant("BitData::exception_seen_flag", Integer.class);
    final int bitDataNullSeenFlag = getConstant("BitData::null_seen_flag", Integer.class);
    final int methodDataCountOffset = getConstant("CounterData::count_off", Integer.class);
    final int jumpDataTakenOffset = getConstant("JumpData::taken_off_set", Integer.class);
    final int jumpDataDisplacementOffset = getConstant("JumpData::displacement_off_set", Integer.class);
    final int receiverTypeDataNonprofiledCountOffset = getConstant("ReceiverTypeData::nonprofiled_count_off_set", Integer.class);
    final int receiverTypeDataReceiverTypeRowCellCount = getConstant("ReceiverTypeData::receiver_type_row_cell_count", Integer.class);
    final int receiverTypeDataReceiver0Offset = getConstant("ReceiverTypeData::receiver0_offset", Integer.class);
    final int receiverTypeDataCount0Offset = getConstant("ReceiverTypeData::count0_offset", Integer.class);
    final int branchDataNotTakenOffset = getConstant("BranchData::not_taken_off_set", Integer.class);
    final int arrayDataArrayLenOffset = getConstant("ArrayData::array_len_off_set", Integer.class);
    final int arrayDataArrayStartOffset = getConstant("ArrayData::array_start_off_set", Integer.class);
    final int multiBranchDataPerCaseCellCount = getConstant("MultiBranchData::per_case_cell_count", Integer.class);
}
