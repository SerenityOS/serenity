/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.InvalidInstalledCodeException;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.stack.InspectedFrameVisitor;
import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.runtime.JVMCI;
import java.lang.reflect.Executable;

/**
 * A simple "proxy" class to get test access to CompilerToVM package-private methods
 */
public class CompilerToVMHelper {
    public static final CompilerToVM CTVM = new CompilerToVM();
    private static final MetaAccessProvider metaAccess = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();

    public static byte[] getBytecode(HotSpotResolvedJavaMethod method) {
        return CTVM.getBytecode((HotSpotResolvedJavaMethodImpl)method);
    }

    public static int getExceptionTableLength(HotSpotResolvedJavaMethod method) {
        return CTVM.getExceptionTableLength((HotSpotResolvedJavaMethodImpl)method);
    }

    public static long getExceptionTableStart(HotSpotResolvedJavaMethod method) {
        return CTVM.getExceptionTableStart((HotSpotResolvedJavaMethodImpl)method);
    }

    public static Object getFlagValue(String name) {
        return CTVM.getFlagValue(name);
    }

    public static boolean isCompilable(HotSpotResolvedJavaMethod method) {
        return CTVM.isCompilable((HotSpotResolvedJavaMethodImpl)method);
    }

    public static boolean hasNeverInlineDirective(HotSpotResolvedJavaMethod method) {
        return CTVM.hasNeverInlineDirective((HotSpotResolvedJavaMethodImpl)method);
    }

    public static boolean shouldInlineMethod(HotSpotResolvedJavaMethod method) {
        return CTVM.shouldInlineMethod((HotSpotResolvedJavaMethodImpl)method);
    }

    public static HotSpotResolvedJavaMethod findUniqueConcreteMethod(
            HotSpotResolvedObjectType actualHolderType,
            HotSpotResolvedJavaMethod method) {
        return CTVM.findUniqueConcreteMethod((HotSpotResolvedObjectTypeImpl) actualHolderType, (HotSpotResolvedJavaMethodImpl)method);
    }

    public static HotSpotResolvedObjectType getImplementor(HotSpotResolvedObjectType type) {
        return CTVM.getImplementor((HotSpotResolvedObjectTypeImpl) type);
    }

    public static boolean methodIsIgnoredBySecurityStackWalk(HotSpotResolvedJavaMethod method) {
        return CTVM.methodIsIgnoredBySecurityStackWalk((HotSpotResolvedJavaMethodImpl)method);
    }

    public static HotSpotResolvedObjectType lookupType(String name,
            Class<?> accessClass, boolean resolve) throws ClassNotFoundException {
        if (accessClass == null) {
            throw new NullPointerException();
        }
        HotSpotResolvedObjectTypeImpl accessingClass = (HotSpotResolvedObjectTypeImpl) metaAccess.lookupJavaType(accessClass);
        return (HotSpotResolvedObjectType) CTVM.lookupType(name, accessingClass, resolve);
    }

    public static HotSpotResolvedObjectType lookupTypeHelper(String name,
            Class<?> accessingClass, boolean resolve) {
        try {
            return lookupType(name, accessingClass, resolve);
        } catch (ClassNotFoundException e) {
            throw (NoClassDefFoundError) new NoClassDefFoundError().initCause(e);
        }
    }

    public static Object resolvePossiblyCachedConstantInPool(ConstantPool constantPool, int cpi) {
        DirectHotSpotObjectConstantImpl obj = (DirectHotSpotObjectConstantImpl) CTVM.resolvePossiblyCachedConstantInPool((HotSpotConstantPool) constantPool, cpi);
        return obj.object;
    }

    public static int lookupNameAndTypeRefIndexInPool(ConstantPool constantPool, int cpi) {
        return CTVM.lookupNameAndTypeRefIndexInPool((HotSpotConstantPool) constantPool, cpi);
    }

    public static String lookupNameInPool(ConstantPool constantPool, int cpi) {
        return CTVM.lookupNameInPool((HotSpotConstantPool) constantPool, cpi);
    }

    public static String lookupSignatureInPool(ConstantPool constantPool, int cpi) {
        return CTVM.lookupSignatureInPool((HotSpotConstantPool) constantPool, cpi);
    }

    public static int lookupKlassRefIndexInPool(ConstantPool constantPool, int cpi) {
        return CTVM.lookupKlassRefIndexInPool((HotSpotConstantPool) constantPool, cpi);
    }

    public static Object lookupKlassInPool(ConstantPool constantPool, int cpi) {
        return CTVM.lookupKlassInPool((HotSpotConstantPool) constantPool, cpi);
    }

    public static HotSpotResolvedJavaMethod lookupMethodInPool(
            ConstantPool constantPool, int cpi, byte opcode) {
        return CTVM.lookupMethodInPool((HotSpotConstantPool) constantPool, cpi, opcode);
    }

    public static void resolveInvokeDynamicInPool(
            ConstantPool constantPool, int cpi) {
        CTVM.resolveInvokeDynamicInPool((HotSpotConstantPool) constantPool, cpi);
    }

    public static void resolveInvokeHandleInPool(
            ConstantPool constantPool, int cpi) {
        CTVM.resolveInvokeHandleInPool((HotSpotConstantPool) constantPool, cpi);
    }

    public static HotSpotResolvedObjectType resolveTypeInPool(
            ConstantPool constantPool, int cpi) {
        return CTVM.resolveTypeInPool((HotSpotConstantPool) constantPool, cpi);
    }

    public static HotSpotResolvedObjectType resolveFieldInPool(
            ConstantPool constantPool, int cpi, ResolvedJavaMethod method, byte opcode, int[] info) {
        return CTVM.resolveFieldInPool((HotSpotConstantPool) constantPool, cpi, (HotSpotResolvedJavaMethodImpl) method, opcode, info);
    }

    public static int constantPoolRemapInstructionOperandFromCache(
            ConstantPool constantPool, int cpci) {
        return CTVM.constantPoolRemapInstructionOperandFromCache((HotSpotConstantPool) constantPool, cpci);
    }

    public static Object lookupAppendixInPool(
            ConstantPool constantPool, int cpi) {
        return CTVM.lookupAppendixInPool((HotSpotConstantPool) constantPool, cpi);
    }

    public static int installCode(TargetDescription target,
            HotSpotCompiledCode compiledCode, InstalledCode code, HotSpotSpeculationLog speculationLog) {
        byte[] speculations;
        long failedSpeculationsAddress;
        if (speculationLog != null) {
            speculations = speculationLog.getFlattenedSpeculations(true);
            failedSpeculationsAddress = speculationLog.getFailedSpeculationsAddress();
        } else {
            speculations = new byte[0];
            failedSpeculationsAddress = 0L;
        }
        return CTVM.installCode(target, compiledCode, code, failedSpeculationsAddress, speculations);
    }

    public static int getMetadata(TargetDescription target,
            HotSpotCompiledCode compiledCode, HotSpotMetaData metaData) {
        return CTVM.getMetadata(target, compiledCode, metaData);
    }

    public static void resetCompilationStatistics() {
        CTVM.resetCompilationStatistics();
    }

    public static Object[] readConfiguration() {
        return CTVM.readConfiguration();
    }

    public static HotSpotResolvedJavaMethod resolveMethod(
            HotSpotResolvedObjectType exactReceiver,
            HotSpotResolvedJavaMethod method,
            HotSpotResolvedObjectType caller) {
        return CTVM.resolveMethod((HotSpotResolvedObjectTypeImpl) exactReceiver, (HotSpotResolvedJavaMethodImpl) method, (HotSpotResolvedObjectTypeImpl) caller);
    }

    public static HotSpotResolvedJavaMethod getClassInitializer(
            HotSpotResolvedObjectType type) {
        return CTVM.getClassInitializer((HotSpotResolvedObjectTypeImpl) type);
    }

    public static boolean hasFinalizableSubclass(HotSpotResolvedObjectType type) {
        return CTVM.hasFinalizableSubclass((HotSpotResolvedObjectTypeImpl) type);
    }

    public static HotSpotResolvedJavaMethodImpl asResolvedJavaMethod(
            Executable executable) {
        return CTVM.asResolvedJavaMethod(executable);
    }

    public static long getMaxCallTargetOffset(long address) {
        return CTVM.getMaxCallTargetOffset(address);
    }

    public static String disassembleCodeBlob(InstalledCode codeBlob) {
        return CTVM.disassembleCodeBlob(codeBlob);
    }

    public static StackTraceElement getStackTraceElement(
            HotSpotResolvedJavaMethod method, int bci) {
        return CTVM.getStackTraceElement((HotSpotResolvedJavaMethodImpl)method, bci);
    }

    public static Object executeHotSpotNmethod(Object[] args,
            HotSpotNmethod nmethodMirror) throws InvalidInstalledCodeException {
        return CTVM.executeHotSpotNmethod(args, nmethodMirror);
    }

    public static long[] getLineNumberTable(HotSpotResolvedJavaMethod method) {
        return CTVM.getLineNumberTable((HotSpotResolvedJavaMethodImpl)method);
    }

    public static int getLocalVariableTableLength(HotSpotResolvedJavaMethod method) {
        return CTVM.getLocalVariableTableLength((HotSpotResolvedJavaMethodImpl)method);
    }

    public static long getLocalVariableTableStart(HotSpotResolvedJavaMethod method) {
        return CTVM.getLocalVariableTableStart((HotSpotResolvedJavaMethodImpl)method);
    }

    public static void setNotInlinableOrCompilable(HotSpotResolvedJavaMethod method) {
        CTVM.setNotInlinableOrCompilable((HotSpotResolvedJavaMethodImpl)method);
    }

    public static void reprofile(HotSpotResolvedJavaMethod method) {
        CTVM.reprofile((HotSpotResolvedJavaMethodImpl)method);
    }

    public static void invalidateHotSpotNmethod(HotSpotNmethod nmethodMirror) {
        CTVM.invalidateHotSpotNmethod(nmethodMirror);
    }

    public static long[] collectCounters() {
        return CTVM.collectCounters();
    }

    public static boolean isMature(long metaspaceMethodData) {
        return CTVM.isMature(metaspaceMethodData);
    }

    public static int allocateCompileId(HotSpotResolvedJavaMethod method,
            int entryBCI) {
        return CTVM.allocateCompileId((HotSpotResolvedJavaMethodImpl) method, entryBCI);
    }

    public static boolean hasCompiledCodeForOSR(
            HotSpotResolvedJavaMethod method, int entryBCI, int level) {
        return CTVM.hasCompiledCodeForOSR((HotSpotResolvedJavaMethodImpl) method, entryBCI, level);
    }

    public static String getSymbol(long metaspaceSymbol) {
        return CTVM.getSymbol(metaspaceSymbol);
    }

    public static <T> T iterateFrames(
            ResolvedJavaMethod[] initialMethods,
            ResolvedJavaMethod[] matchingMethods,
            int initialSkip,
            InspectedFrameVisitor<T> visitor) {
        return CTVM.iterateFrames(initialMethods, matchingMethods, initialSkip, visitor);
    }

    public static void materializeVirtualObjects(
            HotSpotStackFrameReference stackFrame, boolean invalidate) {
        CTVM.materializeVirtualObjects(stackFrame, invalidate);
    }

    public static int getVtableIndexForInterfaceMethod(HotSpotResolvedObjectType type,
            HotSpotResolvedJavaMethod method) {
        return CTVM.getVtableIndexForInterfaceMethod((HotSpotResolvedObjectTypeImpl) type, (HotSpotResolvedJavaMethodImpl) method);
    }

    public static boolean shouldDebugNonSafepoints() {
        return CTVM.shouldDebugNonSafepoints();
    }

    public static void flushDebugOutput() {
        CTVM.flushDebugOutput();
    }

    public static HotSpotResolvedJavaMethod getResolvedJavaMethod(HotSpotObjectConstantImpl base,
            long displacement) {
        return CTVM.getResolvedJavaMethod(base, displacement);
    }

    public static HotSpotConstantPool getConstantPool(MetaspaceObject object) {
        return CTVM.getConstantPool(object);
    }

    public static HotSpotResolvedObjectType getResolvedJavaType(MetaspaceObject base,
            long displacement, boolean compressed) {
        return CTVM.getResolvedJavaType(base, displacement, compressed);
    }

    public static long getMetaspacePointer(Object o) {
        return ((MetaspaceObject) o).getMetaspacePointer();
    }

    public static Class<?> CompilerToVMClass() {
        return CompilerToVM.class;
    }

    public static JavaConstant getJavaMirror(HotSpotResolvedObjectType type) {
        return ((HotSpotResolvedJavaType) type).getJavaMirror();
    }

    public static HotSpotResolvedObjectType fromObjectClass(Class<?> theClass) {
          return (HotSpotResolvedObjectType) metaAccess.lookupJavaType(theClass);
    }

    public static InstalledCode getInstalledCode(ResolvedJavaMethod method, String name, long address, long entryPoint) {
        return new InstalledCodeStub((HotSpotResolvedJavaMethodImpl) method, name, address, entryPoint);
    }
    private static class InstalledCodeStub extends HotSpotNmethod {
        private InstalledCodeStub(HotSpotResolvedJavaMethodImpl method, String name, long address, long entryPoint) {
            super(method, name, false, 0);
            this.address = address;
            this.entryPoint = entryPoint;
        }
    }
}
