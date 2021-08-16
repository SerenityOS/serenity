/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.beans;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;

import sun.reflect.generics.reflectiveObjects.GenericArrayTypeImpl;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

/**
 * This is utility class to resolve types.
 *
 * @since 1.7
 *
 * @author Eamonn McManus
 * @author Sergey Malenkov
 */
public final class TypeResolver {

    private static final WeakCache<Type, Map<Type, Type>> CACHE = new WeakCache<>();

    /**
     * Replaces the given {@code type} in an inherited method
     * with the actual type it has in the given {@code inClass}.
     *
     * <p>Although type parameters are not inherited by subclasses in the Java
     * language, they <em>are</em> effectively inherited when using reflection.
     * For example, if you declare an interface like this...</p>
     *
     * <pre>
     * public interface StringToIntMap extends Map&lt;String,Integer> {}
     * </pre>
     *
     * <p>...then StringToIntMap.class.getMethods() will show that it has methods
     * like put(K,V) even though StringToIntMap has no type parameters.  The K
     * and V variables are the ones declared by Map, so
     * {@link TypeVariable#getGenericDeclaration()} will return Map.class.</p>
     *
     * <p>The purpose of this method is to take a Type from a possibly-inherited
     * method and replace it with the correct Type for the inheriting class.
     * So given parameters of K and StringToIntMap.class in the above example,
     * this method will return String.</p>
     *
     * @param inClass  the base class used to resolve
     * @param type     the type to resolve
     * @return a resolved type
     *
     * @see #getActualType(Class)
     * @see #resolve(Type,Type)
     */
    public static Type resolveInClass(Class<?> inClass, Type type) {
        return resolve(getActualType(inClass), type);
    }

    /**
     * Replaces all {@code types} in the given array
     * with the actual types they have in the given {@code inClass}.
     *
     * @param inClass  the base class used to resolve
     * @param types    the array of types to resolve
     * @return an array of resolved types
     *
     * @see #getActualType(Class)
     * @see #resolve(Type,Type[])
     */
    public static Type[] resolveInClass(Class<?> inClass, Type[] types) {
        return resolve(getActualType(inClass), types);
    }

    /**
     * Replaces type variables of the given {@code formal} type
     * with the types they stand for in the given {@code actual} type.
     *
     * <p>A ParameterizedType is a class with type parameters, and the values
     * of those parameters.  For example, Map&lt;K,V> is a generic class, and
     * a corresponding ParameterizedType might look like
     * Map&lt;K=String,V=Integer>.  Given such a ParameterizedType, this method
     * will replace K with String, or List&lt;K> with List&ltString;, or
     * List&lt;? super K> with List&lt;? super String>.</p>
     *
     * <p>The {@code actual} argument to this method can also be a Class.
     * In this case, either it is equivalent to a ParameterizedType with
     * no parameters (for example, Integer.class), or it is equivalent to
     * a "raw" ParameterizedType (for example, Map.class).  In the latter
     * case, every type parameter declared or inherited by the class is replaced
     * by its "erasure".  For a type parameter declared as &lt;T>, the erasure
     * is Object.  For a type parameter declared as &lt;T extends Number>,
     * the erasure is Number.</p>
     *
     * <p>Although type parameters are not inherited by subclasses in the Java
     * language, they <em>are</em> effectively inherited when using reflection.
     * For example, if you declare an interface like this...</p>
     *
     * <pre>
     * public interface StringToIntMap extends Map&lt;String,Integer> {}
     * </pre>
     *
     * <p>...then StringToIntMap.class.getMethods() will show that it has methods
     * like put(K,V) even though StringToIntMap has no type parameters.  The K
     * and V variables are the ones declared by Map, so
     * {@link TypeVariable#getGenericDeclaration()} will return {@link Map Map.class}.</p>
     *
     * <p>For this reason, this method replaces inherited type parameters too.
     * Therefore if this method is called with {@code actual} being
     * StringToIntMap.class and {@code formal} being the K from Map,
     * it will return {@link String String.class}.</p>
     *
     * <p>In the case where {@code actual} is a "raw" ParameterizedType, the
     * inherited type parameters will also be replaced by their erasures.
     * The erasure of a Class is the Class itself, so a "raw" subinterface of
     * StringToIntMap will still show the K from Map as String.class.  But
     * in a case like this...
     *
     * <pre>
     * public interface StringToIntListMap extends Map&lt;String,List&lt;Integer>> {}
     * public interface RawStringToIntListMap extends StringToIntListMap {}
     * </pre>
     *
     * <p>...the V inherited from Map will show up as List&lt;Integer> in
     * StringToIntListMap, but as plain List in RawStringToIntListMap.</p>
     *
     * @param actual  the type that supplies bindings for type variables
     * @param formal  the type where occurrences of the variables
     *                in {@code actual} will be replaced by the corresponding bound values
     * @return a resolved type
     */
    public static Type resolve(Type actual, Type formal) {
        if (formal instanceof Class) {
            return formal;
        }
        if (formal instanceof GenericArrayType) {
            Type comp = ((GenericArrayType) formal).getGenericComponentType();
            comp = resolve(actual, comp);
            return (comp instanceof Class)
                    ? Array.newInstance((Class<?>) comp, 0).getClass()
                    : GenericArrayTypeImpl.make(comp);
        }
        if (formal instanceof ParameterizedType) {
            ParameterizedType fpt = (ParameterizedType) formal;
            Type[] actuals = resolve(actual, fpt.getActualTypeArguments());
            return ParameterizedTypeImpl.make(
                    (Class<?>) fpt.getRawType(), actuals, fpt.getOwnerType());
        }
        if (formal instanceof WildcardType) {
            WildcardType fwt = (WildcardType) formal;
            Type[] upper = resolve(actual, fwt.getUpperBounds());
            Type[] lower = resolve(actual, fwt.getLowerBounds());
            return new WildcardTypeImpl(upper, lower);
        }
        if (formal instanceof TypeVariable) {
            Map<Type, Type> map;
            synchronized (CACHE) {
                map = CACHE.get(actual);
                if (map == null) {
                    map = new HashMap<>();
                    prepare(map, actual);
                    CACHE.put(actual, map);
                }
            }
            Type result = map.get(formal);
            if (result == null || result.equals(formal)) {
                return formal;
            }
            result = fixGenericArray(result);
            // A variable can be bound to another variable that is itself bound
            // to something.  For example, given:
            // class Super<T> {...}
            // class Mid<X> extends Super<T> {...}
            // class Sub extends Mid<String>
            // the variable T is bound to X, which is in turn bound to String.
            // So if we have to resolve T, we need the tail recursion here.
            return resolve(actual, result);
        }
        throw new IllegalArgumentException("Bad Type kind: " + formal.getClass());
    }

    /**
     * Replaces type variables of all formal types in the given array
     * with the types they stand for in the given {@code actual} type.
     *
     * @param actual   the type that supplies bindings for type variables
     * @param formals  the array of types to resolve
     * @return an array of resolved types
     */
    public static Type[] resolve(Type actual, Type[] formals) {
        int length = formals.length;
        Type[] actuals = new Type[length];
        for (int i = 0; i < length; i++) {
            actuals[i] = resolve(actual, formals[i]);
        }
        return actuals;
    }

    /**
     * Converts the given {@code type} to the corresponding class.
     * This method implements the concept of type erasure,
     * that is described in section 4.6 of
     * <cite>The Java Language Specification</cite>.
     *
     * @param type  the array of types to convert
     * @return a corresponding class
     */
    public static Class<?> erase(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            return (Class<?>) pt.getRawType();
        }
        if (type instanceof TypeVariable) {
            TypeVariable<?> tv = (TypeVariable<?>)type;
            Type[] bounds = tv.getBounds();
            return (0 < bounds.length)
                    ? erase(bounds[0])
                    : Object.class;
        }
        if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType)type;
            Type[] bounds = wt.getUpperBounds();
            return (0 < bounds.length)
                    ? erase(bounds[0])
                    : Object.class;
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType)type;
            return Array.newInstance(erase(gat.getGenericComponentType()), 0).getClass();
        }
        throw new IllegalArgumentException("Unknown Type kind: " + type.getClass());
    }

    /**
     * Converts all {@code types} in the given array
     * to the corresponding classes.
     *
     * @param types  the array of types to convert
     * @return an array of corresponding classes
     *
     * @see #erase(Type)
     */
    public static Class<?>[] erase(Type[] types) {
        int length = types.length;
        Class<?>[] classes = new Class<?>[length];
        for (int i = 0; i < length; i++) {
            classes[i] = TypeResolver.erase(types[i]);
        }
        return classes;
    }

    /**
     * Fills the map from type parameters
     * to types as seen by the given {@code type}.
     * The method is recursive because the {@code type}
     * inherits mappings from its parent classes and interfaces.
     * The {@code type} can be either a {@link Class Class}
     * or a {@link ParameterizedType ParameterizedType}.
     * If it is a {@link Class Class}, it is either equivalent
     * to a {@link ParameterizedType ParameterizedType} with no parameters,
     * or it represents the erasure of a {@link ParameterizedType ParameterizedType}.
     *
     * @param map   the mappings of all type variables
     * @param type  the next type in the hierarchy
     */
    private static void prepare(Map<Type, Type> map, Type type) {
        Class<?> raw = (Class<?>)((type instanceof Class<?>)
                ? type
                : ((ParameterizedType)type).getRawType());

        TypeVariable<?>[] formals = raw.getTypeParameters();

        Type[] actuals = (type instanceof Class<?>)
                ? formals
                : ((ParameterizedType)type).getActualTypeArguments();

        assert formals.length == actuals.length;
        for (int i = 0; i < formals.length; i++) {
            map.put(formals[i], actuals[i]);
        }
        Type gSuperclass = raw.getGenericSuperclass();
        if (gSuperclass != null) {
            prepare(map, gSuperclass);
        }
        for (Type gInterface : raw.getGenericInterfaces()) {
            prepare(map, gInterface);
        }
        // If type is the raw version of a parameterized class, we type-erase
        // all of its type variables, including inherited ones.
        if (type instanceof Class<?> && formals.length > 0) {
            for (Map.Entry<Type, Type> entry : map.entrySet()) {
                entry.setValue(erase(entry.getValue()));
            }
        }
    }

    /**
     * Replaces a {@link GenericArrayType GenericArrayType}
     * with plain array class where it is possible.
     * Bug <a href="https://bugs.java.com/view_bug.do?bug_id=5041784">5041784</a>
     * is that arrays of non-generic type sometimes show up
     * as {@link GenericArrayType GenericArrayType} when using reflection.
     * For example, a {@code String[]} might show up
     * as a {@link GenericArrayType GenericArrayType}
     * where {@link GenericArrayType#getGenericComponentType getGenericComponentType}
     * is {@code String.class}.  This violates the specification,
     * which says that {@link GenericArrayType GenericArrayType}
     * is used when the component type is a type variable or parameterized type.
     * We fit the specification here.
     *
     * @param type  the type to fix
     * @return a corresponding type for the generic array type,
     *         or the same type as {@code type}
     */
    private static Type fixGenericArray(Type type) {
        if (type instanceof GenericArrayType) {
            Type comp = ((GenericArrayType)type).getGenericComponentType();
            comp = fixGenericArray(comp);
            if (comp instanceof Class) {
                return Array.newInstance((Class<?>)comp, 0).getClass();
            }
        }
        return type;
    }

    /**
     * Replaces a {@link Class Class} with type parameters
     * with a {@link ParameterizedType ParameterizedType}
     * where every parameter is bound to itself.
     * When calling {@link #resolveInClass} in the context of {@code inClass},
     * we can't just pass {@code inClass} as the {@code actual} parameter,
     * because if {@code inClass} has type parameters
     * that would be interpreted as accessing the raw type,
     * so we would get unwanted erasure.
     * This is why we bind each parameter to itself.
     * If {@code inClass} does have type parameters and has methods
     * where those parameters appear in the return type or argument types,
     * we will correctly leave those types alone.
     *
     * @param inClass  the base class used to resolve
     * @return a parameterized type for the class,
     *         or the same class as {@code inClass}
     */
    private static Type getActualType(Class<?> inClass) {
        Type[] params = inClass.getTypeParameters();
        return (params.length == 0)
                ? inClass
                : ParameterizedTypeImpl.make(
                        inClass, params, inClass.getEnclosingClass());
    }
}
