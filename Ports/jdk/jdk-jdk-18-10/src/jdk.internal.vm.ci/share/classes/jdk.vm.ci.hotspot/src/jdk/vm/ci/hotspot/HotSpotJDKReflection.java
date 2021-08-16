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

import static jdk.vm.ci.hotspot.CompilerToVM.compilerToVM;
import static jdk.vm.ci.hotspot.HotSpotJVMCIRuntime.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;

import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * Implementation of {@link HotSpotJVMCIReflection} in terms of standard JDK reflection API. This is
 * only available when running in the HotSpot heap.
 */
final class HotSpotJDKReflection extends HotSpotJVMCIReflection {

    @Override
    Object resolveObject(HotSpotObjectConstantImpl object) {
        if (object == null) {
            return null;
        }
        return ((DirectHotSpotObjectConstantImpl) object).object;
    }

    @Override
    boolean isInstance(HotSpotResolvedObjectTypeImpl holder, HotSpotObjectConstantImpl obj) {
        Class<?> javaMirror = getMirror(holder);
        Object value = resolveObject(obj);
        return javaMirror.isInstance(value);
    }

    @Override
    boolean isAssignableFrom(HotSpotResolvedObjectTypeImpl holder, HotSpotResolvedObjectTypeImpl otherType) {
        Class<?> javaMirror = getMirror(holder);
        return javaMirror.isAssignableFrom(getMirror(otherType));

    }

    @Override
    Annotation[] getAnnotations(HotSpotResolvedObjectTypeImpl holder) {
        Class<?> javaMirror = getMirror(holder);
        return javaMirror.getAnnotations();
    }

    @Override
    Annotation[] getDeclaredAnnotations(HotSpotResolvedObjectTypeImpl holder) {
        Class<?> javaMirror = getMirror(holder);
        return javaMirror.getDeclaredAnnotations();
    }

    @Override
    <T extends Annotation> T getAnnotation(HotSpotResolvedObjectTypeImpl holder, Class<T> annotationClass) {
        Class<?> javaMirror = getMirror(holder);
        return javaMirror.getAnnotation(annotationClass);
    }

    @Override
    boolean isLocalClass(HotSpotResolvedObjectTypeImpl holder) {
        Class<?> javaMirror = getMirror(holder);
        return javaMirror.isLocalClass();
    }

    @Override
    boolean isMemberClass(HotSpotResolvedObjectTypeImpl holder) {
        Class<?> javaMirror = getMirror(holder);
        return javaMirror.isMemberClass();
    }

    @Override
    HotSpotResolvedObjectType getEnclosingClass(HotSpotResolvedObjectTypeImpl holder) {
        Class<?> javaMirror = getMirror(holder);
        return (HotSpotResolvedObjectType) runtime().fromClass(javaMirror.getEnclosingClass());
    }

    @Override
    boolean equals(HotSpotObjectConstantImpl a, HotSpotObjectConstantImpl b) {
        return resolveObject(a) == resolveObject(b) && a.isCompressed() == b.isCompressed();
    }

    // This field is being kept around for compatibility with libgraal
    @SuppressWarnings("unused") private long oopSizeOffset;

    @Override
    ResolvedJavaMethod.Parameter[] getParameters(HotSpotResolvedJavaMethodImpl javaMethod) {
        java.lang.reflect.Parameter[] javaParameters = getMethod(javaMethod).getParameters();
        ResolvedJavaMethod.Parameter[] res = new ResolvedJavaMethod.Parameter[javaParameters.length];
        for (int i = 0; i < res.length; i++) {
            java.lang.reflect.Parameter src = javaParameters[i];
            String paramName = src.isNamePresent() ? src.getName() : null;
            res[i] = new ResolvedJavaMethod.Parameter(paramName, src.getModifiers(), javaMethod, i);
        }
        return res;
    }

    @Override
    Annotation[][] getParameterAnnotations(HotSpotResolvedJavaMethodImpl javaMethod) {
        return getMethod(javaMethod).getParameterAnnotations();
    }

    @Override
    Type[] getGenericParameterTypes(HotSpotResolvedJavaMethodImpl javaMethod) {
        return getMethod(javaMethod).getGenericParameterTypes();
    }

    @Override
    Annotation[] getFieldAnnotations(HotSpotResolvedJavaFieldImpl javaField) {
        return getField(javaField).getAnnotations();
    }

    @Override
    Annotation[] getMethodAnnotations(HotSpotResolvedJavaMethodImpl javaMethod) {
        return getMethod(javaMethod).getAnnotations();
    }

    @Override
    Annotation[] getMethodDeclaredAnnotations(HotSpotResolvedJavaMethodImpl javaMethod) {
        return getMethod(javaMethod).getDeclaredAnnotations();
    }

    @Override
    Annotation[] getFieldDeclaredAnnotations(HotSpotResolvedJavaFieldImpl javaField) {
        return getField(javaField).getDeclaredAnnotations();
    }

    @Override
    <T extends Annotation> T getMethodAnnotation(HotSpotResolvedJavaMethodImpl javaMethod, Class<T> annotationClass) {
        return getMethod(javaMethod).getAnnotation(annotationClass);
    }

    @Override
    <T extends Annotation> T getFieldAnnotation(HotSpotResolvedJavaFieldImpl javaField, Class<T> annotationClass) {
        return getField(javaField).getAnnotation(annotationClass);
    }

    @Override
    HotSpotResolvedObjectTypeImpl getType(HotSpotObjectConstantImpl object) {
        Object value = resolveObject(object);
        Class<?> theClass = value.getClass();
        return (HotSpotResolvedObjectTypeImpl) runtime().fromClass(theClass);
    }

    @Override
    String asString(HotSpotObjectConstantImpl object) {
        Object value = resolveObject(object);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    @Override
    ResolvedJavaType asJavaType(HotSpotObjectConstantImpl object) {
        Object value = resolveObject(object);
        if (value instanceof Class) {
            Class<?> javaClass = (Class<?>) value;
            return runtime().fromClass(javaClass);
        }
        if (value instanceof ResolvedJavaType) {
            return (ResolvedJavaType) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    <T> T asObject(HotSpotObjectConstantImpl object, Class<T> type) {
        Object value = resolveObject(object);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    @Override
    Object asObject(HotSpotObjectConstantImpl object, HotSpotResolvedJavaType type) {
        Object value = resolveObject(object);
        if (getMirror(type).isInstance(value)) {
            return value;
        }
        return null;
    }

    @Override
    String formatString(HotSpotObjectConstantImpl object) {
        return JavaKind.Object.format(resolveObject(object));
    }

    @Override
    Integer getLength(HotSpotObjectConstantImpl arrayObject) {
        Object object = resolveObject(arrayObject);
        if (object.getClass().isArray()) {
            return Array.getLength(object);
        }
        return null;
    }

    @Override
    JavaConstant readArrayElement(HotSpotObjectConstantImpl arrayObject, int index) {
        Object a = resolveObject(arrayObject);
        if (!a.getClass().isArray() || index < 0 || index >= Array.getLength(a)) {
            return null;
        }
        if (a instanceof Object[]) {
            Object element = ((Object[]) a)[index];
            return forObject(element);
        } else {
            if (a instanceof int[]) {
                return JavaConstant.forInt(((int[]) a)[index]);
            } else if (a instanceof char[]) {
                return JavaConstant.forChar(((char[]) a)[index]);
            } else if (a instanceof byte[]) {
                return JavaConstant.forByte(((byte[]) a)[index]);
            } else if (a instanceof long[]) {
                return JavaConstant.forLong(((long[]) a)[index]);
            } else if (a instanceof short[]) {
                return JavaConstant.forShort(((short[]) a)[index]);
            } else if (a instanceof float[]) {
                return JavaConstant.forFloat(((float[]) a)[index]);
            } else if (a instanceof double[]) {
                return JavaConstant.forDouble(((double[]) a)[index]);
            } else if (a instanceof boolean[]) {
                return JavaConstant.forBoolean(((boolean[]) a)[index]);
            } else {
                throw new JVMCIError("Should not reach here");
            }
        }
    }

    @Override
    JavaConstant unboxPrimitive(HotSpotObjectConstantImpl source) {
        return JavaConstant.forBoxedPrimitive(resolveObject(source));
    }

    @Override
    JavaConstant forObject(Object value) {
        if (value == null) {
            return JavaConstant.NULL_POINTER;
        }
        return forNonNullObject(value);
    }

    private static HotSpotObjectConstantImpl forNonNullObject(Object value) {
        return DirectHotSpotObjectConstantImpl.forNonNullObject(value, false);
    }

    @Override
    JavaConstant boxPrimitive(JavaConstant source) {
        return forNonNullObject(source.asBoxedPrimitive());
    }

    /**
     * Gets a {@link Method} object corresponding to {@code method}. This method guarantees the same
     * {@link Method} object is returned if called twice on the same {@code method} value.
     */
    private static Executable getMethod(HotSpotResolvedJavaMethodImpl method) {
        assert !method.isClassInitializer() : method;
        if (method.toJavaCache == null) {
            synchronized (method) {
                if (method.toJavaCache == null) {
                    method.toJavaCache = compilerToVM().asReflectionExecutable(method);
                }
            }
        }
        return method.toJavaCache;
    }

    /**
     * Gets a {@link Field} object corresponding to {@code field}. This method guarantees the same
     * {@link Field} object is returned if called twice on the same {@code field} value. This is
     * required to ensure the results of {@link HotSpotResolvedJavaFieldImpl#getAnnotations()} and
     * {@link HotSpotResolvedJavaFieldImpl#getAnnotation(Class)} are stable (i.e., for a given field
     * {@code f} and annotation class {@code a}, the same object is returned for each call to
     * {@code f.getAnnotation(a)}).
     */
    private static Field getField(HotSpotResolvedJavaFieldImpl field) {
        HotSpotResolvedObjectTypeImpl declaringClass = field.getDeclaringClass();
        synchronized (declaringClass) {
            HashMap<HotSpotResolvedJavaFieldImpl, Field> cache = declaringClass.reflectionFieldCache;
            if (cache == null) {
                cache = new HashMap<>();
                declaringClass.reflectionFieldCache = cache;
            }
            Field reflect = cache.get(field);
            if (reflect == null) {
                reflect = compilerToVM().asReflectionField(field.getDeclaringClass(), field.getIndex());
                cache.put(field, reflect);
            }
            return reflect;
        }
    }

    Class<?> getMirror(HotSpotResolvedObjectTypeImpl holder) {
        return (Class<?>) resolveObject((HotSpotObjectConstantImpl) holder.getJavaMirror());
    }

    Class<?> getMirror(HotSpotResolvedJavaType type) {
        assert type != null;
        if (type instanceof HotSpotResolvedPrimitiveType) {
            return (Class<?>) resolveObject(((HotSpotResolvedPrimitiveType) type).mirror);
        } else {
            return getMirror((HotSpotResolvedObjectTypeImpl) type);
        }
    }
}

