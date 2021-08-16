/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import jdk.internal.access.SharedSecrets;
import jdk.internal.access.JavaLangAccess;
import jdk.internal.reflect.ConstantPool;
import static sun.reflect.annotation.TypeAnnotation.*;

/**
 * TypeAnnotationParser implements the logic needed to parse
 * TypeAnnotations from an array of bytes.
 */
public final class TypeAnnotationParser {
    private static final TypeAnnotation[] EMPTY_TYPE_ANNOTATION_ARRAY = new TypeAnnotation[0];

    /**
     * Build an AnnotatedType from the parameters supplied.
     *
     * This method and {@code buildAnnotatedTypes} are probably
     * the entry points you are looking for.
     *
     * @param rawAnnotations the byte[] encoding of all type annotations on this declaration
     * @param cp the ConstantPool needed to parse the embedded Annotation
     * @param decl the declaration this type annotation is on
     * @param container the Class this type annotation is on (may be the same as decl)
     * @param type the type the AnnotatedType corresponds to
     * @param filter the type annotation targets included in this AnnotatedType
     */
    public static AnnotatedType buildAnnotatedType(byte[] rawAnnotations,
            ConstantPool cp,
            AnnotatedElement decl,
            Class<?> container,
            Type type,
            TypeAnnotationTarget filter) {
        TypeAnnotation[] tas = parseTypeAnnotations(rawAnnotations,
                cp, decl, container);

        List<TypeAnnotation> l = new ArrayList<>(tas.length);
        for (TypeAnnotation t : tas) {
            TypeAnnotationTargetInfo ti = t.getTargetInfo();
            if (ti.getTarget() == filter)
                l.add(t);
        }
        TypeAnnotation[] typeAnnotations = l.toArray(EMPTY_TYPE_ANNOTATION_ARRAY);
        return AnnotatedTypeFactory.buildAnnotatedType(type,
                AnnotatedTypeFactory.nestingForType(type, LocationInfo.BASE_LOCATION),
                typeAnnotations,
                typeAnnotations,
                decl);
    }

    /**
     * Build an array of AnnotatedTypes from the parameters supplied.
     *
     * This method and {@code buildAnnotatedType} are probably
     * the entry points you are looking for.
     *
     * @param rawAnnotations the byte[] encoding of all type annotations on this declaration
     * @param cp the ConstantPool needed to parse the embedded Annotation
     * @param decl the declaration this type annotation is on
     * @param container the Class this type annotation is on (may be the same as decl)
     * @param types the Types the AnnotatedTypes corresponds to
     * @param filter the type annotation targets that included in this AnnotatedType
     */
    public static AnnotatedType[] buildAnnotatedTypes(byte[] rawAnnotations,
            ConstantPool cp,
            AnnotatedElement decl,
            Class<?> container,
            Type[] types,
            TypeAnnotationTarget filter) {
        int size = types.length;
        AnnotatedType[] result = new AnnotatedType[size];
        Arrays.fill(result, AnnotatedTypeFactory.EMPTY_ANNOTATED_TYPE);
        @SuppressWarnings("rawtypes")
        ArrayList[] l = new ArrayList[size]; // array of ArrayList<TypeAnnotation>

        TypeAnnotation[] tas = parseTypeAnnotations(rawAnnotations,
                cp, decl, container);

        for (TypeAnnotation t : tas) {
            TypeAnnotationTargetInfo ti = t.getTargetInfo();
            if (ti.getTarget() == filter) {
                int pos = ti.getCount();
                if (l[pos] == null) {
                    ArrayList<TypeAnnotation> tmp = new ArrayList<>(tas.length);
                    l[pos] = tmp;
                }
                @SuppressWarnings("unchecked")
                ArrayList<TypeAnnotation> tmp = l[pos];
                tmp.add(t);
            }
        }
        // If a constructor has a mandated outer this, that parameter
        // has no annotations and the annotations to parameter mapping
        // should be offset by 1.
        boolean offset = false;
        if (decl instanceof Constructor) {
            Constructor<?> ctor = (Constructor<?>) decl;
            Class<?> declaringClass = ctor.getDeclaringClass();
            if (!declaringClass.isEnum() &&
                (declaringClass.isMemberClass() &&
                 (declaringClass.getModifiers() & Modifier.STATIC) == 0) ) {
                offset = true;
            }
        }
        for (int i = 0; i < size; i++) {
            ArrayList<TypeAnnotation> list;
            if (offset) {
                @SuppressWarnings("unchecked")
                ArrayList<TypeAnnotation> tmp = (i == 0) ? null : l[i - 1];
                list = tmp;
            } else {
                @SuppressWarnings("unchecked")
                ArrayList<TypeAnnotation> tmp = l[i];
                list = tmp;
            }
            TypeAnnotation[] typeAnnotations;
            if (list != null) {
                typeAnnotations = list.toArray(new TypeAnnotation[list.size()]);
            } else {
                typeAnnotations = EMPTY_TYPE_ANNOTATION_ARRAY;
            }
            result[i] = AnnotatedTypeFactory.buildAnnotatedType(types[i],
                    AnnotatedTypeFactory.nestingForType(types[i], LocationInfo.BASE_LOCATION),
                    typeAnnotations,
                    typeAnnotations,
                    decl);

        }
        return result;
    }

    // Class helpers

    /**
     * Build an AnnotatedType for the class decl's supertype.
     *
     * @param rawAnnotations the byte[] encoding of all type annotations on this declaration
     * @param cp the ConstantPool needed to parse the embedded Annotation
     * @param decl the Class which annotated supertype is being built
     */
    public static AnnotatedType buildAnnotatedSuperclass(byte[] rawAnnotations,
            ConstantPool cp,
            Class<?> decl) {
        Type supertype = decl.getGenericSuperclass();
        if (supertype == null)
            return AnnotatedTypeFactory.EMPTY_ANNOTATED_TYPE;
        return buildAnnotatedType(rawAnnotations,
                                  cp,
                                  decl,
                                  decl,
                                  supertype,
                                  TypeAnnotationTarget.CLASS_EXTENDS);
    }

    /**
     * Build an array of AnnotatedTypes for the class decl's implemented
     * interfaces.
     *
     * @param rawAnnotations the byte[] encoding of all type annotations on this declaration
     * @param cp the ConstantPool needed to parse the embedded Annotation
     * @param decl the Class whose annotated implemented interfaces is being built
     */
    public static AnnotatedType[] buildAnnotatedInterfaces(byte[] rawAnnotations,
            ConstantPool cp,
            Class<?> decl) {
        if (decl == Object.class ||
                decl.isArray() ||
                decl.isPrimitive() ||
                decl == Void.TYPE)
            return AnnotatedTypeFactory.EMPTY_ANNOTATED_TYPE_ARRAY;
        return buildAnnotatedTypes(rawAnnotations,
                                   cp,
                                   decl,
                                   decl,
                                   decl.getGenericInterfaces(),
                                   TypeAnnotationTarget.CLASS_IMPLEMENTS);
    }

    // TypeVariable helpers

    /**
     * Parse regular annotations on a TypeVariable declared on genericDecl.
     *
     * Regular Annotations on TypeVariables are stored in the type
     * annotation byte[] in the class file.
     *
     * @param genericDecl the declaration declaring the type variable
     * @param typeVarIndex the 0-based index of this type variable in the declaration
     */
    public static <D extends GenericDeclaration> Annotation[] parseTypeVariableAnnotations(D genericDecl,
            int typeVarIndex) {
        AnnotatedElement decl;
        TypeAnnotationTarget predicate;
        if (genericDecl instanceof Class) {
            decl = (Class<?>)genericDecl;
            predicate = TypeAnnotationTarget.CLASS_TYPE_PARAMETER;
        } else if (genericDecl instanceof Executable) {
            decl = (Executable)genericDecl;
            predicate = TypeAnnotationTarget.METHOD_TYPE_PARAMETER;
        } else {
            throw new AssertionError("Unknown GenericDeclaration " + genericDecl + "\nthis should not happen.");
        }
        List<TypeAnnotation> typeVarAnnos = TypeAnnotation.filter(parseAllTypeAnnotations(decl),
                                                                  predicate);
        List<Annotation> res = new ArrayList<>(typeVarAnnos.size());
        for (TypeAnnotation t : typeVarAnnos)
            if (t.getTargetInfo().getCount() == typeVarIndex)
                res.add(t.getAnnotation());
        return res.toArray(new Annotation[0]);
    }

    /**
     * Build an array of AnnotatedTypes for the declaration decl's bounds.
     *
     * @param bounds the bounds corresponding to the annotated bounds
     * @param decl the declaration whose annotated bounds is being built
     * @param typeVarIndex the index of this type variable on the decl
     */
    public static <D extends GenericDeclaration> AnnotatedType[] parseAnnotatedBounds(Type[] bounds,
            D decl,
            int typeVarIndex) {
        return parseAnnotatedBounds(bounds, decl, typeVarIndex, LocationInfo.BASE_LOCATION);
    }
    //helper for above
    private static <D extends GenericDeclaration> AnnotatedType[] parseAnnotatedBounds(Type[] bounds,
            D decl,
            int typeVarIndex,
            LocationInfo loc) {
        List<TypeAnnotation> candidates = fetchBounds(decl);
        if (bounds != null) {
            int startIndex = 0;
            AnnotatedType[] res = new AnnotatedType[bounds.length];

            // According to JVMS 4.3.4, the first bound of a parameterized type is
            // taken to be Object, if no explicit class bound is specified. As a
            // consequence, the first interface's bound is always 1. To account for
            // a potential mismatch between the indices of the bounds array that only
            // contains explicit bounds and the actual bound's index, the startIndex
            // is set to 1 if no explicit class type bound was set.
            //
            // This is achieved by examining the first element of the bound to be a
            // class or an interface, if such a bound exists. Since a bound can itself
            // be a parameterized type, the bound's raw type must be investigated,
            // if applicable.
            if (bounds.length > 0) {
                Type b0 = bounds[0];
                if (b0 instanceof Class<?>) {
                    Class<?> c = (Class<?>) b0;
                    if (c.isInterface()) {
                        startIndex = 1;
                    }
                } else if (b0 instanceof ParameterizedType) {
                    ParameterizedType p = (ParameterizedType) b0;
                    Class<?> c = (Class<?>) p.getRawType();
                    if (c.isInterface()) {
                        startIndex = 1;
                    }
                }
            }

            for (int i = 0; i < bounds.length; i++) {
                List<TypeAnnotation> l = new ArrayList<>(candidates.size());
                for (TypeAnnotation t : candidates) {
                    TypeAnnotationTargetInfo tInfo = t.getTargetInfo();
                    if (tInfo.getSecondaryIndex() == i + startIndex &&
                            tInfo.getCount() == typeVarIndex) {
                        l.add(t);
                    }
                }
                TypeAnnotation[] typeAnnotations = l.toArray(EMPTY_TYPE_ANNOTATION_ARRAY);
                res[i] = AnnotatedTypeFactory.buildAnnotatedType(bounds[i],
                        AnnotatedTypeFactory.nestingForType(bounds[i], loc),
                        typeAnnotations,
                        typeAnnotations,
                        decl);
            }
            return res;
        }
        return new AnnotatedType[0];
    }
    private static <D extends GenericDeclaration> List<TypeAnnotation> fetchBounds(D decl) {
        AnnotatedElement boundsDecl;
        TypeAnnotationTarget target;
        if (decl instanceof Class) {
            target = TypeAnnotationTarget.CLASS_TYPE_PARAMETER_BOUND;
            boundsDecl = (Class)decl;
        } else {
            target = TypeAnnotationTarget.METHOD_TYPE_PARAMETER_BOUND;
            boundsDecl = (Executable)decl;
        }
        return TypeAnnotation.filter(TypeAnnotationParser.parseAllTypeAnnotations(boundsDecl), target);
    }

    /*
     * Parse all type annotations on the declaration supplied. This is needed
     * when you go from for example an annotated return type on a method that
     * is a type variable declared on the class. In this case you need to
     * 'jump' to the decl of the class and parse all type annotations there to
     * find the ones that are applicable to the type variable.
     */
    static TypeAnnotation[] parseAllTypeAnnotations(AnnotatedElement decl) {
        Class<?> container;
        byte[] rawBytes;
        JavaLangAccess javaLangAccess = SharedSecrets.getJavaLangAccess();
        if (decl instanceof Class) {
            container = (Class<?>)decl;
            rawBytes = javaLangAccess.getRawClassTypeAnnotations(container);
        } else if (decl instanceof Executable) {
            container = ((Executable)decl).getDeclaringClass();
            rawBytes = javaLangAccess.getRawExecutableTypeAnnotations((Executable)decl);
        } else {
            // Should not reach here. Assert?
            return EMPTY_TYPE_ANNOTATION_ARRAY;
        }
        return parseTypeAnnotations(rawBytes, javaLangAccess.getConstantPool(container),
                                    decl, container);
    }

    /* Parse type annotations encoded as an array of bytes */
    private static TypeAnnotation[] parseTypeAnnotations(byte[] rawAnnotations,
            ConstantPool cp,
            AnnotatedElement baseDecl,
            Class<?> container) {
        if (rawAnnotations == null)
            return EMPTY_TYPE_ANNOTATION_ARRAY;

        ByteBuffer buf = ByteBuffer.wrap(rawAnnotations);
        int annotationCount = buf.getShort() & 0xFFFF;
        List<TypeAnnotation> typeAnnotations = new ArrayList<>(annotationCount);

        // Parse each TypeAnnotation
        for (int i = 0; i < annotationCount; i++) {
             TypeAnnotation ta = parseTypeAnnotation(buf, cp, baseDecl, container);
             if (ta != null)
                 typeAnnotations.add(ta);
        }

        return typeAnnotations.toArray(EMPTY_TYPE_ANNOTATION_ARRAY);
    }


    // Helper
    static Map<Class<? extends Annotation>, Annotation> mapTypeAnnotations(TypeAnnotation[] typeAnnos) {
        Map<Class<? extends Annotation>, Annotation> result =
            new LinkedHashMap<>();
        for (TypeAnnotation t : typeAnnos) {
            Annotation a = t.getAnnotation();
            if (a != null) {
                Class<? extends Annotation> klass = a.annotationType();
                AnnotationType type = AnnotationType.getInstance(klass);
                if (type.retention() == RetentionPolicy.RUNTIME &&
                    result.put(klass, a) != null) {
                    throw new AnnotationFormatError("Duplicate annotation for class: "+klass+": " + a);
                }
            }
        }
        return result;
    }

    // Position codes
    // Regular type parameter annotations
    private static final byte CLASS_TYPE_PARAMETER = 0x00;
    private static final byte METHOD_TYPE_PARAMETER = 0x01;
    // Type Annotations outside method bodies
    private static final byte CLASS_EXTENDS = 0x10;
    private static final byte CLASS_TYPE_PARAMETER_BOUND = 0x11;
    private static final byte METHOD_TYPE_PARAMETER_BOUND = 0x12;
    private static final byte FIELD = 0x13;
    private static final byte METHOD_RETURN = 0x14;
    private static final byte METHOD_RECEIVER = 0x15;
    private static final byte METHOD_FORMAL_PARAMETER = 0x16;
    private static final byte THROWS = 0x17;
    // Type Annotations inside method bodies
    private static final byte LOCAL_VARIABLE = (byte)0x40;
    private static final byte RESOURCE_VARIABLE = (byte)0x41;
    private static final byte EXCEPTION_PARAMETER = (byte)0x42;
    private static final byte INSTANCEOF = (byte)0x43;
    private static final byte NEW = (byte)0x44;
    private static final byte CONSTRUCTOR_REFERENCE = (byte)0x45;
    private static final byte METHOD_REFERENCE = (byte)0x46;
    private static final byte CAST = (byte)0x47;
    private static final byte CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = (byte)0x48;
    private static final byte METHOD_INVOCATION_TYPE_ARGUMENT = (byte)0x49;
    private static final byte CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT = (byte)0x4A;
    private static final byte METHOD_REFERENCE_TYPE_ARGUMENT = (byte)0x4B;

    private static TypeAnnotation parseTypeAnnotation(ByteBuffer buf,
            ConstantPool cp,
            AnnotatedElement baseDecl,
            Class<?> container) {
        try {
            TypeAnnotationTargetInfo ti = parseTargetInfo(buf);
            LocationInfo locationInfo = LocationInfo.parseLocationInfo(buf);
            Annotation a = AnnotationParser.parseAnnotation(buf, cp, container, false);
            if (ti == null) // Inside a method for example
                return null;
            return new TypeAnnotation(ti, locationInfo, a, baseDecl);
        } catch (IllegalArgumentException | // Bad type in const pool at specified index
                BufferUnderflowException e) {
            throw new AnnotationFormatError(e);
        }
    }

    private static TypeAnnotationTargetInfo parseTargetInfo(ByteBuffer buf) {
        int posCode = buf.get() & 0xFF;
        switch(posCode) {
        case CLASS_TYPE_PARAMETER:
        case METHOD_TYPE_PARAMETER: {
            int index = buf.get() & 0xFF;
            TypeAnnotationTargetInfo res;
            if (posCode == CLASS_TYPE_PARAMETER)
                res = new TypeAnnotationTargetInfo(TypeAnnotationTarget.CLASS_TYPE_PARAMETER,
                        index);
            else
                res = new TypeAnnotationTargetInfo(TypeAnnotationTarget.METHOD_TYPE_PARAMETER,
                        index);
            return res;
            } // unreachable break;
        case CLASS_EXTENDS: {
            short index = buf.getShort(); //needs to be signed
            if (index == -1) {
                return new TypeAnnotationTargetInfo(TypeAnnotationTarget.CLASS_EXTENDS);
            } else if (index >= 0) {
                TypeAnnotationTargetInfo res = new TypeAnnotationTargetInfo(TypeAnnotationTarget.CLASS_IMPLEMENTS,
                        index);
                return res;
            }} break;
        case CLASS_TYPE_PARAMETER_BOUND:
            return parse2ByteTarget(TypeAnnotationTarget.CLASS_TYPE_PARAMETER_BOUND, buf);
        case METHOD_TYPE_PARAMETER_BOUND:
            return parse2ByteTarget(TypeAnnotationTarget.METHOD_TYPE_PARAMETER_BOUND, buf);
        case FIELD:
            return new TypeAnnotationTargetInfo(TypeAnnotationTarget.FIELD);
        case METHOD_RETURN:
            return new TypeAnnotationTargetInfo(TypeAnnotationTarget.METHOD_RETURN);
        case METHOD_RECEIVER:
            return new TypeAnnotationTargetInfo(TypeAnnotationTarget.METHOD_RECEIVER);
        case METHOD_FORMAL_PARAMETER: {
            int index = buf.get() & 0xFF;
            return new TypeAnnotationTargetInfo(TypeAnnotationTarget.METHOD_FORMAL_PARAMETER,
                    index);
            } //unreachable break;
        case THROWS:
            return parseShortTarget(TypeAnnotationTarget.THROWS, buf);

        /*
         * The ones below are inside method bodies, we don't care about them for core reflection
         * other than adjusting for them in the byte stream.
         */
        case LOCAL_VARIABLE:
        case RESOURCE_VARIABLE:
            short length = buf.getShort();
            for (int i = 0; i < length; ++i) {
                short offset = buf.getShort();
                short varLength = buf.getShort();
                short index = buf.getShort();
            }
            return null;
        case EXCEPTION_PARAMETER: {
            byte index = buf.get();
            }
            return null;
        case INSTANCEOF:
        case NEW:
        case CONSTRUCTOR_REFERENCE:
        case METHOD_REFERENCE: {
            short offset = buf.getShort();
            }
            return null;
        case CAST:
        case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
        case METHOD_INVOCATION_TYPE_ARGUMENT:
        case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
        case METHOD_REFERENCE_TYPE_ARGUMENT: {
            short offset = buf.getShort();
            byte index = buf.get();
            }
            return null;

        default:
            // will throw error below
            break;
        }
        throw new AnnotationFormatError("Could not parse bytes for type annotations");
    }

    private static TypeAnnotationTargetInfo parseShortTarget(TypeAnnotationTarget target, ByteBuffer buf) {
        int index = buf.getShort() & 0xFFFF;
        return new TypeAnnotationTargetInfo(target, index);
    }
    private static TypeAnnotationTargetInfo parse2ByteTarget(TypeAnnotationTarget target, ByteBuffer buf) {
        int count = buf.get() & 0xFF;
        int secondaryIndex = buf.get() & 0xFF;
        return new TypeAnnotationTargetInfo(target,
                                            count,
                                            secondaryIndex);
    }
}
