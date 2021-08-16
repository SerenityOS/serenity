/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.reflect.generics.reflectiveObjects;

import java.lang.annotation.*;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import sun.reflect.annotation.AnnotationSupport;
import sun.reflect.annotation.TypeAnnotationParser;
import sun.reflect.annotation.AnnotationType;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.tree.FieldTypeSignature;
import sun.reflect.generics.visitor.Reifier;
import sun.reflect.misc.ReflectUtil;

/**
 * Implementation of {@code java.lang.reflect.TypeVariable} interface
 * for core reflection.
 */
public class TypeVariableImpl<D extends GenericDeclaration>
    extends LazyReflectiveObjectGenerator implements TypeVariable<D> {
    private final D genericDeclaration;
    private final String name;

    /**
     * The upper bounds.  Lazily converted from FieldTypeSignature[] to Type[].
     * We are required to evaluate the bounds lazily, so we store them as ASTs
     * until we are first asked for them.  This also neatly solves the problem
     * with F-bounds - you can't reify them before the formal is defined.
     */
    private volatile Object[] bounds;

    // constructor is private to enforce access through static factory
    private TypeVariableImpl(D decl, String n, FieldTypeSignature[] bs,
                             GenericsFactory f) {
        super(f);
        genericDeclaration = decl;
        name = n;
        bounds = bs;
    }

    /**
     * Factory method.
     * @param decl - the reflective object that declared the type variable
     * that this method should create
     * @param name - the name of the type variable to be returned
     * @param bs - an array of ASTs representing the bounds for the type
     * variable to be created
     * @param f - a factory that can be used to manufacture reflective
     * objects that represent the bounds of this type variable
     * @return A type variable with name, bounds, declaration and factory
     * specified
     */
    public static <T extends GenericDeclaration>
                             TypeVariableImpl<T> make(T decl, String name,
                                                      FieldTypeSignature[] bs,
                                                      GenericsFactory f) {

        if (!((decl instanceof Class) ||
                (decl instanceof Method) ||
                (decl instanceof Constructor))) {
            throw new AssertionError("Unexpected kind of GenericDeclaration" +
                    decl.getClass().toString());
        }
        return new TypeVariableImpl<T>(decl, name, bs, f);
    }


    /**
     * Returns an array of {@code Type} objects representing the
     * upper bound(s) of this type variable.  Note that if no upper bound is
     * explicitly declared, the upper bound is {@code Object}.
     *
     * <p>For each upper bound B:
     * <ul>
     *  <li>if B is a parameterized type or a type variable, it is created,
     *  (see {@link #ParameterizedType} for the details of the creation
     *  process for parameterized types).
     *  <li>Otherwise, B is resolved.
     * </ul>
     *
     * @throws {@code TypeNotPresentException} if any of the
     *     bounds refers to a non-existent type declaration
     * @throws {@code MalformedParameterizedTypeException} if any of the
     *     bounds refer to a parameterized type that cannot be instantiated
     *     for any reason
     * @return an array of Types representing the upper bound(s) of this
     *     type variable
     */
    public Type[] getBounds() {
        Object[] value = bounds;
        if (value instanceof FieldTypeSignature[]) {
            value = reifyBounds((FieldTypeSignature[])value);
            bounds = value;
        }
        return (Type[])value.clone();
    }

    /**
     * Returns the {@code GenericDeclaration} object representing the
     * generic declaration that declared this type variable.
     *
     * @return the generic declaration that declared this type variable.
     *
     * @since 1.5
     */
    public D getGenericDeclaration() {
        if (genericDeclaration instanceof Class)
            ReflectUtil.checkPackageAccess((Class)genericDeclaration);
        else if ((genericDeclaration instanceof Method) ||
                (genericDeclaration instanceof Constructor))
            ReflectUtil.conservativeCheckMemberAccess((Member)genericDeclaration);
        else
            throw new AssertionError("Unexpected kind of GenericDeclaration");
        return genericDeclaration;
    }


    /**
     * Returns the name of this type variable, as it occurs in the source code.
     *
     * @return the name of this type variable, as it appears in the source code
     */
    public String getName()   { return name; }

    public String toString() {return getName();}

    @Override
    public boolean equals(Object o) {
        if (o instanceof TypeVariable &&
                o.getClass() == TypeVariableImpl.class) {
            TypeVariable<?> that = (TypeVariable<?>) o;

            GenericDeclaration thatDecl = that.getGenericDeclaration();
            String thatName = that.getName();

            return Objects.equals(genericDeclaration, thatDecl) &&
                Objects.equals(name, thatName);

        } else
            return false;
    }

    @Override
    public int hashCode() {
        return genericDeclaration.hashCode() ^ name.hashCode();
    }

    // Implementations of AnnotatedElement methods.
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        // T is an Annotation type, the return value of get will be an annotation
        return (T)mapAnnotations(getAnnotations()).get(annotationClass);
    }

    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        return getAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        return AnnotationSupport.getDirectlyAndIndirectlyPresent(mapAnnotations(getAnnotations()), annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        return getAnnotationsByType(annotationClass);
    }

    public Annotation[] getAnnotations() {
        int myIndex = typeVarIndex();
        if (myIndex < 0)
            throw new AssertionError("Index must be non-negative.");
        return TypeAnnotationParser.parseTypeVariableAnnotations(getGenericDeclaration(), myIndex);
    }

    public Annotation[] getDeclaredAnnotations() {
        return getAnnotations();
    }

    public AnnotatedType[] getAnnotatedBounds() {
        return TypeAnnotationParser.parseAnnotatedBounds(getBounds(),
                                                         getGenericDeclaration(),
                                                         typeVarIndex());
    }

    private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

    // Helpers for annotation methods
    private int typeVarIndex() {
        TypeVariable<?>[] tVars = getGenericDeclaration().getTypeParameters();
        int i = -1;
        for (TypeVariable<?> v : tVars) {
            i++;
            if (equals(v))
                return i;
        }
        return -1;
    }

    private static Map<Class<? extends Annotation>, Annotation> mapAnnotations(Annotation[] annos) {
        Map<Class<? extends Annotation>, Annotation> result =
            new LinkedHashMap<>();
        for (Annotation a : annos) {
            Class<? extends Annotation> klass = a.annotationType();
            AnnotationType type = AnnotationType.getInstance(klass);
            if (type.retention() == RetentionPolicy.RUNTIME)
                if (result.put(klass, a) != null)
                    throw new AnnotationFormatError("Duplicate annotation for class: "+klass+": " + a);
        }
        return result;
    }
}
