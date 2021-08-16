/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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

package sun.reflect.generics.factory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;


import sun.reflect.generics.reflectiveObjects.*;
import sun.reflect.generics.scope.Scope;
import sun.reflect.generics.tree.FieldTypeSignature;


/**
 * Factory for reflective generic type objects for use by
 * core reflection (java.lang.reflect).
 */
public class CoreReflectionFactory implements GenericsFactory {
    private final GenericDeclaration decl;
    private final Scope scope;

    private CoreReflectionFactory(GenericDeclaration d, Scope s) {
        decl = d;
        scope = s;
    }

    private GenericDeclaration getDecl(){ return decl;}

    private Scope getScope(){ return scope;}


    private ClassLoader getDeclsLoader() {
        if (decl instanceof Class) {return ((Class) decl).getClassLoader();}
        if (decl instanceof Method) {
            return ((Method) decl).getDeclaringClass().getClassLoader();
        }
        assert decl instanceof Constructor : "Constructor expected";
        return ((Constructor) decl).getDeclaringClass().getClassLoader();

    }

    /**
     * Factory for this class. Returns an instance of
     * {@code CoreReflectionFactory} for the declaration and scope
     * provided.
     * This factory will produce reflective objects of the appropriate
     * kind. Classes produced will be those that would be loaded by the
     * defining class loader of the declaration {@code d} (if {@code d}
     * is a type declaration, or by the defining loader of the declaring
     * class of {@code d} otherwise.
     * <p> Type variables will be created or lookup as necessary in the
     * scope {@code s}.
     * @param d - the generic declaration (class, interface, method or
     * constructor) that this factory services
     * @param s  the scope in which the factory will allocate and search for
     * type variables
     * @return an instance of {@code CoreReflectionFactory}
     */
    public static CoreReflectionFactory make(GenericDeclaration d, Scope s) {
        return new CoreReflectionFactory(d, s);
    }

    public TypeVariable<?> makeTypeVariable(String name,
                                            FieldTypeSignature[] bounds){
        return TypeVariableImpl.make(getDecl(), name, bounds, this);
    }

    public WildcardType makeWildcard(FieldTypeSignature[] ubs,
                                     FieldTypeSignature[] lbs) {
        return WildcardTypeImpl.make(ubs, lbs, this);
    }

    public ParameterizedType makeParameterizedType(Type declaration,
                                                   Type[] typeArgs,
                                                   Type owner) {
        return ParameterizedTypeImpl.make((Class<?>) declaration,
                                          typeArgs, owner);
    }

    public TypeVariable<?> findTypeVariable(String name){
        return getScope().lookup(name);
    }

    public Type makeNamedType(String name){
        try {return Class.forName(name, false, // don't initialize
                                  getDeclsLoader());}
        catch (ClassNotFoundException c) {
            throw new TypeNotPresentException(name, c);
        }
    }

    public Type makeArrayType(Type componentType){
        if (componentType instanceof Class<?>)
            return Array.newInstance((Class<?>) componentType, 0).getClass();
        else
            return GenericArrayTypeImpl.make(componentType);
    }

    public Type makeByte(){return byte.class;}
    public Type makeBool(){return boolean.class;}
    public Type makeShort(){return short.class;}
    public Type makeChar(){return char.class;}
    public Type makeInt(){return int.class;}
    public Type makeLong(){return long.class;}
    public Type makeFloat(){return float.class;}
    public Type makeDouble(){return double.class;}

    public Type makeVoid(){return void.class;}
}
