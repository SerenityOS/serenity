/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.reflect.annotation;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jdk.internal.access.SharedSecrets;
import jdk.internal.access.JavaLangAccess;
import jdk.internal.reflect.ReflectionFactory;

public final class AnnotationSupport {
    private static final JavaLangAccess LANG_ACCESS = SharedSecrets.getJavaLangAccess();

    /**
     * Finds and returns all annotations in {@code annotations} matching
     * the given {@code annoClass}.
     *
     * Apart from annotations directly present in {@code annotations} this
     * method searches for annotations inside containers i.e. indirectly
     * present annotations.
     *
     * The order of the elements in the array returned depends on the iteration
     * order of the provided map. Specifically, the directly present annotations
     * come before the indirectly present annotations if and only if the
     * directly present annotations come before the indirectly present
     * annotations in the map.
     *
     * @param annotations the {@code Map} in which to search for annotations
     * @param annoClass the type of annotation to search for
     *
     * @return an array of instances of {@code annoClass} or an empty
     *         array if none were found
     */
    public static <A extends Annotation> A[] getDirectlyAndIndirectlyPresent(
            Map<Class<? extends Annotation>, Annotation> annotations,
            Class<A> annoClass) {
        List<A> result = new ArrayList<>();

        @SuppressWarnings("unchecked")
        A direct = (A) annotations.get(annoClass);
        if (direct != null)
            result.add(direct);

        A[] indirect = getIndirectlyPresent(annotations, annoClass);
        if (indirect != null && indirect.length != 0) {
            boolean indirectFirst = direct == null ||
                                    containerBeforeContainee(annotations, annoClass);

            result.addAll((indirectFirst ? 0 : 1), Arrays.asList(indirect));
        }

        @SuppressWarnings("unchecked")
        A[] arr = (A[]) Array.newInstance(annoClass, result.size());
        return result.toArray(arr);
    }

    /**
     * Finds and returns all annotations matching the given {@code annoClass}
     * indirectly present in {@code annotations}.
     *
     * @param annotations annotations to search indexed by their types
     * @param annoClass the type of annotation to search for
     *
     * @return an array of instances of {@code annoClass} or an empty array if no
     *         indirectly present annotations were found
     */
    private static <A extends Annotation> A[] getIndirectlyPresent(
            Map<Class<? extends Annotation>, Annotation> annotations,
            Class<A> annoClass) {

        Repeatable repeatable = annoClass.getDeclaredAnnotation(Repeatable.class);
        if (repeatable == null)
            return null;  // Not repeatable -> no indirectly present annotations

        Class<? extends Annotation> containerClass = repeatable.value();

        Annotation container = annotations.get(containerClass);
        if (container == null)
            return null;

        // Unpack container
        A[] valueArray = getValueArray(container);
        checkTypes(valueArray, container, annoClass);

        return valueArray;
    }


    /**
     * Figures out if container class comes before containee class among the
     * keys of the given map.
     *
     * @return true if container class is found before containee class when
     *         iterating over annotations.keySet().
     */
    private static <A extends Annotation> boolean containerBeforeContainee(
            Map<Class<? extends Annotation>, Annotation> annotations,
            Class<A> annoClass) {

        Class<? extends Annotation> containerClass =
                annoClass.getDeclaredAnnotation(Repeatable.class).value();

        for (Class<? extends Annotation> c : annotations.keySet()) {
            if (c == containerClass) return true;
            if (c == annoClass) return false;
        }

        // Neither containee nor container present
        return false;
    }


    /**
     * Finds and returns all associated annotations matching the given class.
     *
     * The order of the elements in the array returned depends on the iteration
     * order of the provided maps. Specifically, the directly present annotations
     * come before the indirectly present annotations if and only if the
     * directly present annotations come before the indirectly present
     * annotations in the relevant map.
     *
     * @param declaredAnnotations the declared annotations indexed by their types
     * @param decl the class declaration on which to search for annotations
     * @param annoClass the type of annotation to search for
     *
     * @return an array of instances of {@code annoClass} or an empty array if none were found.
     */
    public static <A extends Annotation> A[] getAssociatedAnnotations(
            Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
            Class<?> decl,
            Class<A> annoClass) {
        Objects.requireNonNull(decl);

        // Search declared
        A[] result = getDirectlyAndIndirectlyPresent(declaredAnnotations, annoClass);

        // Search inherited
        if(AnnotationType.getInstance(annoClass).isInherited()) {
            Class<?> superDecl = decl.getSuperclass();
            while (result.length == 0 && superDecl != null) {
                result = getDirectlyAndIndirectlyPresent(LANG_ACCESS.getDeclaredAnnotationMap(superDecl), annoClass);
                superDecl = superDecl.getSuperclass();
            }
        }

        return result;
    }


    /* Reflectively invoke the values-method of the given annotation
     * (container), cast it to an array of annotations and return the result.
     */
    @SuppressWarnings("removal")
    private static <A extends Annotation> A[] getValueArray(Annotation container) {
        try {
            // According to JLS the container must have an array-valued value
            // method. Get the AnnotationType, get the "value" method and invoke
            // it to get the content.

            Class<? extends Annotation> containerClass = container.annotationType();
            AnnotationType annoType = AnnotationType.getInstance(containerClass);
            if (annoType == null)
                throw invalidContainerException(container, null);
            Method m = annoType.members().get("value");
            if (m == null)
                throw invalidContainerException(container, null);

            if (Proxy.isProxyClass(container.getClass())) {
                // Invoke by invocation handler
                InvocationHandler handler = Proxy.getInvocationHandler(container);

                try {
                    // This will erase to (Annotation[]) but we do a runtime cast on the
                    // return-value in the method that call this method.
                    @SuppressWarnings("unchecked")
                    A[] values = (A[]) handler.invoke(container, m, null);
                    return values;
                } catch (Throwable t) { // from InvocationHandler::invoke
                    throw invalidContainerException(container, t);
                }
            } else {
                // In theory there might be instances of Annotations that are not
                // implemented using Proxies. Try to invoke the "value" element with
                // reflection.

                // Declaring class should be an annotation type
                Class<?> iface = m.getDeclaringClass();
                if (!iface.isAnnotation())
                    throw new UnsupportedOperationException("Unsupported container annotation type.");
                // Method must be public
                if (!Modifier.isPublic(m.getModifiers()))
                    throw new UnsupportedOperationException("Unsupported value member.");

                // Interface might not be public though
                final Method toInvoke;
                if (!Modifier.isPublic(iface.getModifiers())) {
                    if (System.getSecurityManager() != null) {
                        toInvoke = AccessController.doPrivileged(new PrivilegedAction<Method>() {
                            @Override
                            public Method run() {
                                Method res = ReflectionFactory.getReflectionFactory().leafCopyMethod(m);
                                res.setAccessible(true);
                                return res;
                            }
                        });
                    } else {
                        toInvoke = ReflectionFactory.getReflectionFactory().leafCopyMethod(m);
                        toInvoke.setAccessible(true);
                    }
                } else {
                    toInvoke = m;
                }

                // This will erase to (Annotation[]) but we do a runtime cast on the
                // return-value in the method that call this method.
                @SuppressWarnings("unchecked")
                A[] values = (A[]) toInvoke.invoke(container);

                return values;
            }
        } catch (IllegalAccessException    | // couldn't loosen security
                 IllegalArgumentException  | // parameters doesn't match
                 InvocationTargetException | // the value method threw an exception
                 ClassCastException e) {
            throw invalidContainerException(container, e);
        }
    }


    private static AnnotationFormatError invalidContainerException(Annotation anno,
                                                                   Throwable cause) {
        return new AnnotationFormatError(
                anno + " is an invalid container for repeating annotations",
                cause);
    }


    /* Sanity check type of all the annotation instances of type {@code annoClass}
     * from {@code container}.
     */
    private static <A extends Annotation> void checkTypes(A[] annotations,
                                                          Annotation container,
                                                          Class<A> annoClass) {
        for (A a : annotations) {
            if (!annoClass.isInstance(a)) {
                throw new AnnotationFormatError(
                        String.format("%s is an invalid container for " +
                                      "repeating annotations of type: %s",
                                      container, annoClass));
            }
        }
    }
}
