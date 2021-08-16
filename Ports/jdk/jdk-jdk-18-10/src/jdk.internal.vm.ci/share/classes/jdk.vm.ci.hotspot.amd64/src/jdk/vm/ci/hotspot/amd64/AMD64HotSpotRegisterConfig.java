/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.hotspot.amd64;

import static jdk.vm.ci.amd64.AMD64.r12;
import static jdk.vm.ci.amd64.AMD64.r15;
import static jdk.vm.ci.amd64.AMD64.r8;
import static jdk.vm.ci.amd64.AMD64.r9;
import static jdk.vm.ci.amd64.AMD64.rax;
import static jdk.vm.ci.amd64.AMD64.rcx;
import static jdk.vm.ci.amd64.AMD64.rdi;
import static jdk.vm.ci.amd64.AMD64.rdx;
import static jdk.vm.ci.amd64.AMD64.rsi;
import static jdk.vm.ci.amd64.AMD64.rsp;
import static jdk.vm.ci.amd64.AMD64.xmm0;
import static jdk.vm.ci.amd64.AMD64.xmm1;
import static jdk.vm.ci.amd64.AMD64.xmm2;
import static jdk.vm.ci.amd64.AMD64.xmm3;
import static jdk.vm.ci.amd64.AMD64.xmm4;
import static jdk.vm.ci.amd64.AMD64.xmm5;
import static jdk.vm.ci.amd64.AMD64.xmm6;
import static jdk.vm.ci.amd64.AMD64.xmm7;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CallingConvention.Type;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterAttributes;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueKindFactory;
import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

public class AMD64HotSpotRegisterConfig implements RegisterConfig {

    private final TargetDescription target;

    private final RegisterArray allocatable;

    /**
     * The caller saved registers always include all parameter registers.
     */
    private final RegisterArray callerSaved;

    private final boolean allAllocatableAreCallerSaved;

    private final RegisterAttributes[] attributesMap;

    @Override
    public RegisterArray getAllocatableRegisters() {
        return allocatable;
    }

    @Override
    public RegisterArray filterAllocatableRegisters(PlatformKind kind, RegisterArray registers) {
        ArrayList<Register> list = new ArrayList<>();
        for (Register reg : registers) {
            if (target.arch.canStoreValue(reg.getRegisterCategory(), kind)) {
                list.add(reg);
            }
        }

        RegisterArray ret = new RegisterArray(list);
        return ret;
    }

    @Override
    public RegisterAttributes[] getAttributesMap() {
        return attributesMap.clone();
    }

    private final RegisterArray javaGeneralParameterRegisters;
    private final RegisterArray nativeGeneralParameterRegisters;
    private final RegisterArray javaXMMParameterRegisters;
    private final RegisterArray nativeXMMParameterRegisters;
    private final boolean windowsOS;

    /*
     * Some ABIs (e.g. Windows) require a so-called "home space", that is a save area on the stack
     * to store the argument registers
     */
    private final boolean needsNativeStackHomeSpace;

    private static final RegisterArray reservedRegisters = new RegisterArray(rsp, r15);

    private static RegisterArray initAllocatable(Architecture arch, boolean reserveForHeapBase) {
        RegisterArray allRegisters = arch.getAvailableValueRegisters();
        Register[] registers = new Register[allRegisters.size() - reservedRegisters.size() - (reserveForHeapBase ? 1 : 0)];
        List<Register> reservedRegistersList = reservedRegisters.asList();

        int idx = 0;
        for (Register reg : allRegisters) {
            if (reservedRegistersList.contains(reg)) {
                // skip reserved registers
                continue;
            }
            if (reserveForHeapBase && reg.equals(r12)) {
                // skip heap base register
                continue;
            }

            registers[idx++] = reg;
        }

        assert idx == registers.length;
        return new RegisterArray(registers);
    }

    public AMD64HotSpotRegisterConfig(TargetDescription target, boolean useCompressedOops, boolean windowsOs) {
        this(target, initAllocatable(target.arch, useCompressedOops), windowsOs);
        assert callerSaved.size() >= allocatable.size();
    }

    public AMD64HotSpotRegisterConfig(TargetDescription target, RegisterArray allocatable, boolean windowsOS) {
        this.target = target;
        this.windowsOS = windowsOS;

        if (windowsOS) {
            javaGeneralParameterRegisters = new RegisterArray(rdx, r8, r9, rdi, rsi, rcx);
            nativeGeneralParameterRegisters = new RegisterArray(rcx, rdx, r8, r9);
            nativeXMMParameterRegisters = new RegisterArray(xmm0, xmm1, xmm2, xmm3);
            this.needsNativeStackHomeSpace = true;
        } else {
            javaGeneralParameterRegisters = new RegisterArray(rsi, rdx, rcx, r8, r9, rdi);
            nativeGeneralParameterRegisters = new RegisterArray(rdi, rsi, rdx, rcx, r8, r9);
            nativeXMMParameterRegisters = new RegisterArray(xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7);
            this.needsNativeStackHomeSpace = false;
        }
        javaXMMParameterRegisters = new RegisterArray(xmm0, xmm1, xmm2, xmm3, xmm4, xmm5, xmm6, xmm7);

        this.allocatable = allocatable;
        Set<Register> callerSaveSet = new HashSet<>();
        allocatable.addTo(callerSaveSet);
        javaXMMParameterRegisters.addTo(callerSaveSet);
        callerSaveSet.addAll(javaGeneralParameterRegisters.asList());
        nativeGeneralParameterRegisters.addTo(callerSaveSet);
        callerSaved = new RegisterArray(callerSaveSet);

        allAllocatableAreCallerSaved = true;
        attributesMap = RegisterAttributes.createMap(this, target.arch.getRegisters());
    }

    @Override
    public RegisterArray getCallerSaveRegisters() {
        return callerSaved;
    }

    @Override
    public RegisterArray getCalleeSaveRegisters() {
        return null;
    }

    @Override
    public boolean areAllAllocatableRegistersCallerSaved() {
        return allAllocatableAreCallerSaved;
    }

    @Override
    public CallingConvention getCallingConvention(Type type, JavaType returnType, JavaType[] parameterTypes, ValueKindFactory<?> valueKindFactory) {
        HotSpotCallingConventionType hotspotType = (HotSpotCallingConventionType) type;
        if (type == HotSpotCallingConventionType.NativeCall) {
            return callingConvention(nativeGeneralParameterRegisters, nativeXMMParameterRegisters, windowsOS, returnType, parameterTypes, hotspotType, valueKindFactory);
        }
        // On x64, parameter locations are the same whether viewed
        // from the caller or callee perspective
        return callingConvention(javaGeneralParameterRegisters, javaXMMParameterRegisters, false, returnType, parameterTypes, hotspotType, valueKindFactory);
    }

    @Override
    public RegisterArray getCallingConventionRegisters(Type type, JavaKind kind) {
        HotSpotCallingConventionType hotspotType = (HotSpotCallingConventionType) type;
        switch (kind) {
            case Boolean:
            case Byte:
            case Short:
            case Char:
            case Int:
            case Long:
            case Object:
                return hotspotType == HotSpotCallingConventionType.NativeCall ? nativeGeneralParameterRegisters : javaGeneralParameterRegisters;
            case Float:
            case Double:
                return hotspotType == HotSpotCallingConventionType.NativeCall ? nativeXMMParameterRegisters : javaXMMParameterRegisters;
            default:
                throw JVMCIError.shouldNotReachHere();
        }
    }

    /**
     * Hand out registers matching the calling convention from the {@code generalParameterRegisters}
     * and {@code xmmParameterRegisters} sets. Normally registers are handed out from each set
     * individually based on the type of the argument. If the {@code unified} flag is true then hand
     * out registers in a single sequence, selecting between the sets based on the type. This is to
     * support the Windows calling convention which only ever passes 4 arguments in registers, no
     * matter their types.
     *
     * @param generalParameterRegisters
     * @param xmmParameterRegisters
     * @param unified
     * @param returnType
     * @param parameterTypes
     * @param type
     * @param valueKindFactory
     * @return the resulting calling convention
     */
    private CallingConvention callingConvention(RegisterArray generalParameterRegisters, RegisterArray xmmParameterRegisters, boolean unified, JavaType returnType, JavaType[] parameterTypes,
                    HotSpotCallingConventionType type,
                    ValueKindFactory<?> valueKindFactory) {
        assert !unified || generalParameterRegisters.size() == xmmParameterRegisters.size() : "must be same size in unified mode";
        AllocatableValue[] locations = new AllocatableValue[parameterTypes.length];

        int currentGeneral = 0;
        int currentXMM = 0;
        int currentStackOffset = type == HotSpotCallingConventionType.NativeCall && needsNativeStackHomeSpace ? generalParameterRegisters.size() * target.wordSize : 0;

        for (int i = 0; i < parameterTypes.length; i++) {
            final JavaKind kind = parameterTypes[i].getJavaKind().getStackKind();

            switch (kind) {
                case Byte:
                case Boolean:
                case Short:
                case Char:
                case Int:
                case Long:
                case Object:
                    if (currentGeneral < generalParameterRegisters.size()) {
                        Register register = generalParameterRegisters.get(currentGeneral++);
                        locations[i] = register.asValue(valueKindFactory.getValueKind(kind));
                    }
                    break;
                case Float:
                case Double:
                    if ((unified ? currentGeneral : currentXMM) < xmmParameterRegisters.size()) {
                        Register register = xmmParameterRegisters.get(unified ? currentGeneral++ : currentXMM++);
                        locations[i] = register.asValue(valueKindFactory.getValueKind(kind));
                    }
                    break;
                default:
                    throw JVMCIError.shouldNotReachHere();
            }

            if (locations[i] == null) {
                ValueKind<?> valueKind = valueKindFactory.getValueKind(kind);
                locations[i] = StackSlot.get(valueKind, currentStackOffset, !type.out);
                currentStackOffset += Math.max(valueKind.getPlatformKind().getSizeInBytes(), target.wordSize);
            }
        }
        assert !unified || currentXMM == 0 : "shouldn't be used in unified mode";

        JavaKind returnKind = returnType == null ? JavaKind.Void : returnType.getJavaKind();
        AllocatableValue returnLocation = returnKind == JavaKind.Void ? Value.ILLEGAL : getReturnRegister(returnKind).asValue(valueKindFactory.getValueKind(returnKind.getStackKind()));
        return new CallingConvention(currentStackOffset, returnLocation, locations);
    }

    @Override
    public Register getReturnRegister(JavaKind kind) {
        switch (kind) {
            case Boolean:
            case Byte:
            case Char:
            case Short:
            case Int:
            case Long:
            case Object:
                return rax;
            case Float:
            case Double:
                return xmm0;
            case Void:
            case Illegal:
                return null;
            default:
                throw new UnsupportedOperationException("no return register for type " + kind);
        }
    }

    @Override
    public Register getFrameRegister() {
        return rsp;
    }

    @Override
    public String toString() {
        return String.format("Allocatable: " + getAllocatableRegisters() + "%n" + "CallerSave:  " + getCallerSaveRegisters() + "%n");
    }
}
