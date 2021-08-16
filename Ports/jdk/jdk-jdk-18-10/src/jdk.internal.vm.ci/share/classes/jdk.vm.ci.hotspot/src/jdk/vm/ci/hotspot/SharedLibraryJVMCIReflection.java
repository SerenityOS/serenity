/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * Implementation of {@link HotSpotJVMCIReflection} when running in a JVMCI shared library.
 */
class SharedLibraryJVMCIReflection extends HotSpotJVMCIReflection {

    @Override
    Object resolveObject(HotSpotObjectConstantImpl objectHandle) {
        throw new HotSpotJVMCIUnsupportedOperationError("cannot resolve handle in a JVMCI shared library to a raw object: " + objectHandle);
    }

    @Override
    boolean isInstance(HotSpotResolvedObjectTypeImpl holder, HotSpotObjectConstantImpl obj) {
        if (obj instanceof DirectHotSpotObjectConstantImpl) {
            ResolvedJavaType type = getType(obj);
            return holder.isAssignableFrom(type);
        }
        return runtime().compilerToVm.isInstance(holder, obj);
    }

    @Override
    boolean isAssignableFrom(HotSpotResolvedObjectTypeImpl holder, HotSpotResolvedObjectTypeImpl otherType) {
        return runtime().compilerToVm.isAssignableFrom(holder, otherType);
    }

    @Override
    boolean isLocalClass(HotSpotResolvedObjectTypeImpl holder) {
        throw new HotSpotJVMCIUnsupportedOperationError("requires a call Class.isLocalClass()");
    }

    @Override
    boolean isMemberClass(HotSpotResolvedObjectTypeImpl holder) {
        throw new HotSpotJVMCIUnsupportedOperationError("requires a call Class.isMemberClass()");
    }

    @Override
    HotSpotResolvedObjectType getEnclosingClass(HotSpotResolvedObjectTypeImpl holder) {
        throw new HotSpotJVMCIUnsupportedOperationError("requires a call Class.getEnclosingClass()");
    }

    @Override
    boolean equals(HotSpotObjectConstantImpl x, HotSpotObjectConstantImpl y) {
        if (x == y) {
            return true;
        }
        if (x.compressed != y.compressed) {
            return false;
        }
        if (x instanceof DirectHotSpotObjectConstantImpl && y instanceof DirectHotSpotObjectConstantImpl) {
            DirectHotSpotObjectConstantImpl xd = (DirectHotSpotObjectConstantImpl) x;
            DirectHotSpotObjectConstantImpl yd = (DirectHotSpotObjectConstantImpl) y;
            return (xd.object == yd.object);
        }
        if (x instanceof DirectHotSpotObjectConstantImpl || y instanceof DirectHotSpotObjectConstantImpl) {
            // Mixing of constant types is always inequal
            return false;
        }
        IndirectHotSpotObjectConstantImpl indirectX = (IndirectHotSpotObjectConstantImpl) x;
        IndirectHotSpotObjectConstantImpl indirectY = (IndirectHotSpotObjectConstantImpl) y;
        return runtime().compilerToVm.equals(x, indirectX.getHandle(), y, indirectY.getHandle());
    }

    @Override
    ResolvedJavaMethod.Parameter[] getParameters(HotSpotResolvedJavaMethodImpl javaMethod) {
        // ResolvedJavaMethod.getParameters allows a return value of null
        return null;
    }

    // Substituted by Target_jdk_vm_ci_hotspot_SharedLibraryJVMCIReflection
    static Annotation[] getClassAnnotations(String className) {
        throw new InternalError("missing substitution: " + className);
    }

    // Substituted by Target_jdk_vm_ci_hotspot_SharedLibraryJVMCIReflection
    static Annotation[][] getParameterAnnotations(String className, String methodName) {
        throw new InternalError("missing substitution: " + className + " " + methodName);
    }

    @Override
    Annotation[] getAnnotations(HotSpotResolvedObjectTypeImpl holder) {
        Annotation[] annotations = getClassAnnotations(holder.getName());
        return annotations == null ? new Annotation[0] : annotations;
    }

    @Override
    Annotation[] getDeclaredAnnotations(HotSpotResolvedObjectTypeImpl holder) {
        throw new HotSpotJVMCIUnsupportedOperationError("unimplemented");
    }

    @Override
    <T extends Annotation> T getAnnotation(HotSpotResolvedObjectTypeImpl holder, Class<T> annotationClass) {
        throw new HotSpotJVMCIUnsupportedOperationError("unimplemented");
    }

    @Override
    Annotation[][] getParameterAnnotations(HotSpotResolvedJavaMethodImpl javaMethod) {
        Annotation[][] annotations = getParameterAnnotations(javaMethod.getDeclaringClass().getName(), javaMethod.getName());
        if (annotations == null) {
            return new Annotation[javaMethod.signature.getParameterCount(false)][0];
        }
        return annotations;
    }

    @Override
    Type[] getGenericParameterTypes(HotSpotResolvedJavaMethodImpl javaMethod) {
        throw new HotSpotJVMCIUnsupportedOperationError("unimplemented");
    }

    @Override
    Annotation[] getFieldAnnotations(HotSpotResolvedJavaFieldImpl javaField) {
        throw new HotSpotJVMCIUnsupportedOperationError("unimplemented");
    }

    @Override
    Annotation[] getMethodAnnotations(HotSpotResolvedJavaMethodImpl javaMethod) {
        Annotation[] annotations = getMethodAnnotationsInternal(javaMethod);
        return annotations == null ? new Annotation[0] : annotations;
    }

    @Override
    <T extends Annotation> T getMethodAnnotation(HotSpotResolvedJavaMethodImpl javaMethod, Class<T> annotationClass) {
        Annotation[] methodAnnotations = getMethodAnnotations(javaMethod);
        if (methodAnnotations != null) {
            for (Annotation ann : methodAnnotations) {
                if (annotationClass.isInstance(ann)) {
                    return annotationClass.cast(ann);
                }
            }
        }
        return null;
    }

    // Substituted by Target_jdk_vm_ci_hotspot_SharedLibraryJVMCIReflection
    @SuppressWarnings("unused")
    private static Annotation[] getMethodAnnotationsInternal(ResolvedJavaMethod javaMethod) {
        throw new InternalError("missing substitution");
    }

    @Override
    Annotation[] getMethodDeclaredAnnotations(HotSpotResolvedJavaMethodImpl javaMethod) {
        throw new HotSpotJVMCIUnsupportedOperationError("unimplemented");
    }

    @Override
    Annotation[] getFieldDeclaredAnnotations(HotSpotResolvedJavaFieldImpl javaMethod) {
        throw new HotSpotJVMCIUnsupportedOperationError("unimplemented");
    }

    @Override
    <T extends Annotation> T getFieldAnnotation(HotSpotResolvedJavaFieldImpl javaField, Class<T> annotationClass) {
        throw new HotSpotJVMCIUnsupportedOperationError("unimplemented");
    }

    @Override
    HotSpotResolvedObjectTypeImpl getType(HotSpotObjectConstantImpl object) {
        if (object instanceof DirectHotSpotObjectConstantImpl) {
            Class<?> theClass = ((DirectHotSpotObjectConstantImpl) object).object.getClass();
            try {
                String name = theClass.getName().replace('.', '/');
                HotSpotResolvedObjectTypeImpl type = (HotSpotResolvedObjectTypeImpl) runtime().compilerToVm.lookupType(name, null, true);
                if (type == null) {
                    throw new InternalError(name);
                }
                return type;
            } catch (ClassNotFoundException e) {
                throw new InternalError(e);
            }
        }
        return runtime().compilerToVm.getResolvedJavaType(object, runtime().getConfig().hubOffset, false);
    }

    @Override
    String asString(HotSpotObjectConstantImpl object) {
        if (object instanceof IndirectHotSpotObjectConstantImpl) {
            return runtime().compilerToVm.asString(object);
        }
        Object value = ((DirectHotSpotObjectConstantImpl) object).object;
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    @Override
    ResolvedJavaType asJavaType(HotSpotObjectConstantImpl object) {
        if (object instanceof DirectHotSpotObjectConstantImpl) {
            DirectHotSpotObjectConstantImpl direct = (DirectHotSpotObjectConstantImpl) object;
            if (direct.object instanceof Class) {
                Class<?> javaClass = (Class<?>) direct.object;
                return runtime().fromClass(javaClass);
            }
            if (direct.object instanceof ResolvedJavaType) {
                return (ResolvedJavaType) convertUnknownValue(direct.object);
            }
            return null;
        }
        return runtime().compilerToVm.asJavaType(object);
    }

    // Substituted by Target_jdk_vm_ci_hotspot_SharedLibraryJVMCIReflection
    static Object convertUnknownValue(Object object) {
        return object;
    }

    @SuppressWarnings("unchecked")
    @Override
    <T> T asObject(HotSpotObjectConstantImpl object, Class<T> type) {
        if (object instanceof DirectHotSpotObjectConstantImpl) {
            Object theObject = ((DirectHotSpotObjectConstantImpl) object).object;
            if (type.isInstance(theObject)) {
                return (T) convertUnknownValue(type.cast(theObject));
            }
        }
        return null;
    }

    @Override
    Object asObject(HotSpotObjectConstantImpl object, HotSpotResolvedJavaType type) {
        throw new HotSpotJVMCIUnsupportedOperationError("cannot resolve a shared library JVMCI object handle to a " +
                        "raw object as it may be in another runtime");
    }

    @Override
    String formatString(HotSpotObjectConstantImpl object) {
        if (object instanceof DirectHotSpotObjectConstantImpl) {
            DirectHotSpotObjectConstantImpl direct = (DirectHotSpotObjectConstantImpl) object;
            return "CompilerObject<" + direct.object.getClass().getName() + ">";
        }
        IndirectHotSpotObjectConstantImpl indirect = (IndirectHotSpotObjectConstantImpl) object;
        if (!indirect.isValid()) {
            return "Instance<null>";
        }
        return "Instance<" + object.getType().toJavaName() + ">";
    }

    @Override
    Integer getLength(HotSpotObjectConstantImpl object) {
        if (object instanceof DirectHotSpotObjectConstantImpl) {
            DirectHotSpotObjectConstantImpl direct = (DirectHotSpotObjectConstantImpl) object;
            if (direct.object.getClass().isArray()) {
                return Array.getLength(direct.object);
            }
            return null;
        }
        int length = runtime().compilerToVm.getArrayLength(object);
        if (length >= 0) {
            return length;
        }
        return null;
    }

    @Override
    JavaConstant readArrayElement(HotSpotObjectConstantImpl arrayObject, int index) {
        Object result = runtime().compilerToVm.readArrayElement(arrayObject, index);
        if (result == null) {
            return null;
        }
        if (result instanceof JavaConstant) {
            return (JavaConstant) result;
        }
        JavaConstant constant = JavaConstant.forBoxedPrimitive(result);
        if (constant == null) {
            throw new InternalError("Unexpected value " + result);
        }
        return constant;
    }

    @Override
    JavaConstant forObject(Object value) {
        return DirectHotSpotObjectConstantImpl.forObject(value, false);
    }

    @Override
    JavaConstant unboxPrimitive(HotSpotObjectConstantImpl source) {
        Object box = runtime().compilerToVm.unboxPrimitive(source);
        return JavaConstant.forBoxedPrimitive(box);
    }

    @Override
    JavaConstant boxPrimitive(JavaConstant source) {
        return runtime().compilerToVm.boxPrimitive(source.asBoxedPrimitive());
    }
}
