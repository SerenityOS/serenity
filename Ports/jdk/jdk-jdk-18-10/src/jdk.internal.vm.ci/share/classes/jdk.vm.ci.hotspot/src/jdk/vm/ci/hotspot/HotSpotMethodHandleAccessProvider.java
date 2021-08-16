/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodHandle;

import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.common.NativeImageReinitialize;
import jdk.vm.ci.hotspot.HotSpotMethodData.VMState;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MethodHandleAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

public class HotSpotMethodHandleAccessProvider implements MethodHandleAccessProvider {

    private final ConstantReflectionProvider constantReflection;

    public HotSpotMethodHandleAccessProvider(ConstantReflectionProvider constantReflection) {
        this.constantReflection = constantReflection;
    }

    /**
     * Lazy initialized reflection on {@link MethodHandle} internals. Field and method lookup is
     * only possible after the {@link HotSpotJVMCIRuntime} is fully initialized.
     */
    static final class Internals {
        final ResolvedJavaType lambdaFormType;
        final ResolvedJavaField methodHandleFormField;
        final ResolvedJavaField lambdaFormVmentryField;
        final HotSpotResolvedJavaField callSiteTargetField;
        final HotSpotResolvedJavaField constantCallSiteFrozenField;
        final ResolvedJavaField methodField;
        final HotSpotResolvedJavaField vmtargetField;

        /**
         * Search for an instance field with the given name in a class.
         *
         * @param declaringType the type declaring the field
         * @param fieldName name of the field to be searched
         * @param fieldType resolved Java type of the field
         * @return resolved Java field
         * @throws NoSuchFieldError
         */
        private static ResolvedJavaField findFieldInClass(ResolvedJavaType declaringType, String fieldName, ResolvedJavaType fieldType) {
            ResolvedJavaField[] fields = declaringType.getInstanceFields(false);
            for (ResolvedJavaField field : fields) {
                if (field.getName().equals(fieldName) && field.getType().equals(fieldType)) {
                    return field;
                }
            }
            throw new NoSuchFieldError(declaringType + "." + fieldName);
        }

        private static ResolvedJavaType resolveType(String className) {
            return (ResolvedJavaType) runtime().lookupTypeInternal(className, null, true);
        }

        private Internals() {
            try {
                ResolvedJavaType methodHandleType = resolveType("Ljava/lang/invoke/MethodHandle;");
                ResolvedJavaType memberNameType = resolveType("Ljava/lang/invoke/MemberName;");
                lambdaFormType = resolveType("Ljava/lang/invoke/LambdaForm;");
                methodHandleFormField = findFieldInClass(methodHandleType, "form", lambdaFormType);
                lambdaFormVmentryField = findFieldInClass(lambdaFormType, "vmentry", memberNameType);

                ResolvedJavaType methodType = resolveType("Ljava/lang/invoke/ResolvedMethodName;");
                methodField = findFieldInClass(memberNameType, "method", methodType);
                vmtargetField = (HotSpotResolvedJavaField) findFieldInClass(methodType, "vmtarget", resolveType(Character.toString(HotSpotJVMCIRuntime.getHostWordKind().getTypeChar())));

                ResolvedJavaType callSiteType = resolveType("Ljava/lang/invoke/CallSite;");
                callSiteTargetField = (HotSpotResolvedJavaField) findFieldInClass(callSiteType, "target", methodHandleType);
                ResolvedJavaType constantCallSiteType = resolveType("Ljava/lang/invoke/ConstantCallSite;");
                ResolvedJavaType booleanType = resolveType("Z");
                constantCallSiteFrozenField = (HotSpotResolvedJavaField) findFieldInClass(constantCallSiteType, "isFrozen", booleanType);
            } catch (Throwable ex) {
                throw new JVMCIError(ex);
            }
        }

        /**
         * Singleton instance lazily initialized via double-checked locking.
         */
        @NativeImageReinitialize private static volatile Internals instance;

        static Internals instance() {
            Internals result = instance;
            if (result == null) {
                synchronized (VMState.class) {
                    result = instance;
                    if (result == null) {
                        instance = result = new Internals();
                    }
                }
            }
            return result;
        }

    }


    @Override
    public IntrinsicMethod lookupMethodHandleIntrinsic(ResolvedJavaMethod method) {
        int intrinsicId = ((HotSpotResolvedJavaMethodImpl) method).intrinsicId();
        if (intrinsicId != 0) {
            return getMethodHandleIntrinsic(intrinsicId);
        }
        return null;
    }

    public static IntrinsicMethod getMethodHandleIntrinsic(int intrinsicId) {
        HotSpotVMConfig config = runtime().getConfig();
        if (intrinsicId == config.vmIntrinsicInvokeBasic) {
            return IntrinsicMethod.INVOKE_BASIC;
        } else if (intrinsicId == config.vmIntrinsicLinkToInterface) {
            return IntrinsicMethod.LINK_TO_INTERFACE;
        } else if (intrinsicId == config.vmIntrinsicLinkToSpecial) {
            return IntrinsicMethod.LINK_TO_SPECIAL;
        } else if (intrinsicId == config.vmIntrinsicLinkToStatic) {
            return IntrinsicMethod.LINK_TO_STATIC;
        } else if (intrinsicId == config.vmIntrinsicLinkToVirtual) {
            return IntrinsicMethod.LINK_TO_VIRTUAL;
        }
        return null;
    }

    @Override
    public ResolvedJavaMethod resolveInvokeBasicTarget(JavaConstant methodHandle, boolean forceBytecodeGeneration) {
        if (methodHandle.isNull()) {
            return null;
        }

        /* Load non-public field: LambdaForm MethodHandle.form */
        Internals internals = Internals.instance();
        JavaConstant lambdaForm = constantReflection.readFieldValue(internals.methodHandleFormField, methodHandle);
        if (lambdaForm == null || lambdaForm.isNull()) {
            return null;
        }

        JavaConstant memberName = constantReflection.readFieldValue(internals.lambdaFormVmentryField, lambdaForm);
        if (memberName.isNull() && forceBytecodeGeneration) {
            compilerToVM().compileToBytecode((HotSpotObjectConstantImpl) lambdaForm);
            memberName = constantReflection.readFieldValue(internals.lambdaFormVmentryField, lambdaForm);
            assert memberName.isNonNull();
        }
        JavaConstant method = constantReflection.readFieldValue(internals.methodField, memberName);
        return getTargetMethod(method);
    }

    @Override
    public ResolvedJavaMethod resolveLinkToTarget(JavaConstant memberName) {
        if (memberName.isNull()) {
            return null;
        }
        JavaConstant method = constantReflection.readFieldValue(Internals.instance().methodField, memberName);
        return getTargetMethod(method);
    }

    /**
     * Returns the {@link ResolvedJavaMethod} for the method of a java.lang.invoke.MemberName.
     */
    private static ResolvedJavaMethod getTargetMethod(JavaConstant method) {
        if (method == null) {
            // If readFieldValue returns NULL the type was wrong
            throw new IllegalArgumentException("unexpected type for memberName");
        }

        /* Read the ResolvedJavaMethod from the injected field MemberName.method.vmtarget */
        return compilerToVM().getResolvedJavaMethod((HotSpotObjectConstantImpl) method, Internals.instance().vmtargetField.getOffset());
    }
}
