/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.classfile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 *  Family of classes used to represent the parsed form of a {@link Descriptor}
 *  or {@link Signature}.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public abstract class Type {
    protected Type() { }

    public boolean isObject() {
        return false;
    }

    public abstract <R,D> R accept(Visitor<R,D> visitor, D data);

    protected static void append(StringBuilder sb, String prefix, List<? extends Type> types, String suffix) {
        sb.append(prefix);
        String sep = "";
        for (Type t: types) {
            sb.append(sep);
            sb.append(t);
            sep = ", ";
        }
        sb.append(suffix);
    }

    protected static void appendIfNotEmpty(StringBuilder sb, String prefix, List<? extends Type> types, String suffix) {
        if (types != null && types.size() > 0)
            append(sb, prefix, types, suffix);
    }

    public interface Visitor<R,P> {
        R visitSimpleType(SimpleType type, P p);
        R visitArrayType(ArrayType type, P p);
        R visitMethodType(MethodType type, P p);
        R visitClassSigType(ClassSigType type, P p);
        R visitClassType(ClassType type, P p);
        R visitTypeParamType(TypeParamType type, P p);
        R visitWildcardType(WildcardType type, P p);
    }

    /**
     * Represents a type signature with a simple name. The name may be that of a
     * primitive type, such "{@code int}, {@code float}, etc
     * or that of a type argument, such as {@code T}, {@code K}, {@code V}, etc.
     *
     * See:
     * JVMS 4.3.2
     *      BaseType:
     *          {@code B}, {@code C}, {@code D}, {@code F}, {@code I},
     *          {@code J}, {@code S}, {@code Z};
     *      VoidDescriptor:
     *          {@code V};
     * JVMS 4.3.4
     *      TypeVariableSignature:
     *          {@code T} Identifier {@code ;}
     */
    public static class SimpleType extends Type {
        public SimpleType(String name) {
            this.name = name;
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitSimpleType(this, data);
        }

        public boolean isPrimitiveType() {
            return primitiveTypes.contains(name);
        }
        // where
        private static final Set<String> primitiveTypes = new HashSet<>(Arrays.asList(
            "boolean", "byte", "char", "double", "float", "int", "long", "short", "void"));

        @Override
        public String toString() {
            return name;
        }

        public final String name;
    }

    /**
     * Represents an array type signature.
     *
     * See:
     * JVMS 4.3.4
     *      ArrayTypeSignature:
     *          {@code [} TypeSignature {@code ]}
     */
    public static class ArrayType extends Type {
        public ArrayType(Type elemType) {
            this.elemType = elemType;
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitArrayType(this, data);
        }

        @Override
        public String toString() {
            return elemType + "[]";
        }

        public final Type elemType;
    }

    /**
     * Represents a method type signature.
     *
     * See;
     * JVMS 4.3.4
     *      MethodTypeSignature:
     *          FormalTypeParameters_opt {@code (} TypeSignature* {@code)} ReturnType
     *              ThrowsSignature*
     */
    public static class MethodType extends Type {
        public MethodType(List<? extends Type> paramTypes, Type resultType) {
            this(null, paramTypes, resultType, null);
        }

        public MethodType(List<? extends TypeParamType> typeParamTypes,
                List<? extends Type> paramTypes,
                Type returnType,
                List<? extends Type> throwsTypes) {
            this.typeParamTypes = typeParamTypes;
            this.paramTypes = paramTypes;
            this.returnType = returnType;
            this.throwsTypes = throwsTypes;
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitMethodType(this, data);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            appendIfNotEmpty(sb, "<", typeParamTypes, "> ");
            sb.append(returnType);
            append(sb, " (", paramTypes, ")");
            appendIfNotEmpty(sb, " throws ", throwsTypes, "");
            return sb.toString();
        }

        public final List<? extends TypeParamType> typeParamTypes;
        public final List<? extends Type> paramTypes;
        public final Type returnType;
        public final List<? extends Type> throwsTypes;
    }

    /**
     * Represents a class signature. These describe the signature of
     * a class that has type arguments.
     *
     * See:
     * JVMS 4.3.4
     *      ClassSignature:
     *          FormalTypeParameters_opt SuperclassSignature SuperinterfaceSignature*
     */
    public static class ClassSigType extends Type {
        public ClassSigType(List<TypeParamType> typeParamTypes, Type superclassType,
                List<Type> superinterfaceTypes) {
            this.typeParamTypes = typeParamTypes;
            this.superclassType = superclassType;
            this.superinterfaceTypes = superinterfaceTypes;
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitClassSigType(this, data);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            appendIfNotEmpty(sb, "<", typeParamTypes, ">");
            if (superclassType != null) {
                sb.append(" extends ");
                sb.append(superclassType);
            }
            appendIfNotEmpty(sb, " implements ", superinterfaceTypes, "");
            return sb.toString();
        }

        public final List<TypeParamType> typeParamTypes;
        public final Type superclassType;
        public final List<Type> superinterfaceTypes;
    }

    /**
     * Represents a class type signature. This is used to represent a
     * reference to a class, such as in a field, parameter, return type, etc.
     *
     * See:
     * JVMS 4.3.4
     *      ClassTypeSignature:
     *          {@code L} PackageSpecifier_opt SimpleClassTypeSignature
     *                  ClassTypeSignatureSuffix* {@code ;}
     *      PackageSpecifier:
     *          Identifier {@code /} PackageSpecifier*
     *      SimpleClassTypeSignature:
     *          Identifier TypeArguments_opt }
     *      ClassTypeSignatureSuffix:
     *          {@code .} SimpleClassTypeSignature
     */
    public static class ClassType extends Type {
        public ClassType(ClassType outerType, String name, List<Type> typeArgs) {
            this.outerType = outerType;
            this.name = name;
            this.typeArgs = typeArgs;
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitClassType(this, data);
        }

        public String getBinaryName() {
            if (outerType == null)
                return name;
            else
                return (outerType.getBinaryName() + "$" + name);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (outerType != null) {
                sb.append(outerType);
                sb.append(".");
            }
            sb.append(name);
            appendIfNotEmpty(sb, "<", typeArgs, ">");
            return sb.toString();
        }

        @Override
        public boolean isObject() {
            return (outerType == null)
                    && name.equals("java/lang/Object")
                    && (typeArgs == null || typeArgs.isEmpty());
        }

        public final ClassType outerType;
        public final String name;
        public final List<Type> typeArgs;
    }

    /**
     * Represents a FormalTypeParameter. These are used to declare the type
     * parameters for generic classes and methods.
     *
     * See:
     * JVMS 4.3.4
     *     FormalTypeParameters:
     *          {@code <} FormalTypeParameter+ {@code >}
     *     FormalTypeParameter:
     *          Identifier ClassBound InterfaceBound*
     *     ClassBound:
     *          {@code :} FieldTypeSignature_opt
     *     InterfaceBound:
     *          {@code :} FieldTypeSignature
     */
    public static class TypeParamType extends Type {
        public TypeParamType(String name, Type classBound, List<Type> interfaceBounds) {
            this.name = name;
            this.classBound = classBound;
            this.interfaceBounds = interfaceBounds;
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitTypeParamType(this, data);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            String sep = " extends ";
            if (classBound != null) {
                sb.append(sep);
                sb.append(classBound);
                sep = " & ";
            }
            if (interfaceBounds != null) {
                for (Type bound: interfaceBounds) {
                    sb.append(sep);
                    sb.append(bound);
                    sep = " & ";
                }
            }
            return sb.toString();
        }

        public final String name;
        public final Type classBound;
        public final List<Type> interfaceBounds;
    }

    /**
     * Represents a wildcard type argument.  A type argument that is not a
     * wildcard type argument will be represented by a ClassType, ArrayType, etc.
     *
     * See:
     * JVMS 4.3.4
     *      TypeArgument:
     *          WildcardIndicator_opt FieldTypeSignature
     *          {@code *}
     *      WildcardIndicator:
     *          {@code +}
     *          {@code -}
     */
    public static class WildcardType extends Type {
        public enum Kind { UNBOUNDED, EXTENDS, SUPER }

        public WildcardType() {
            this(Kind.UNBOUNDED, null);
        }
        public WildcardType(Kind kind, Type boundType) {
            this.kind = kind;
            this.boundType = boundType;
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitWildcardType(this, data);
        }

        @Override
        public String toString() {
            switch (kind) {
                case UNBOUNDED:
                    return "?";
                case EXTENDS:
                    return "? extends " + boundType;
                case SUPER:
                    return "? super " + boundType;
                default:
                    throw new AssertionError();
            }
        }

        public final Kind kind;
        public final Type boundType;
    }
}
