/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.annotation.*;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import sun.reflect.annotation.*;

import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.util.*;

import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;
import static com.sun.tools.javac.code.Kinds.Kind.*;

/**
 * A generator of dynamic proxy implementations of
 * java.lang.annotation.Annotation.
 *
 * <p> The "dynamic proxy return form" of an annotation element value is
 * the form used by sun.reflect.annotation.AnnotationInvocationHandler.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */

public class AnnotationProxyMaker {

    private final Attribute.Compound anno;
    private final Class<? extends Annotation> annoType;


    private AnnotationProxyMaker(Attribute.Compound anno,
                                 Class<? extends Annotation> annoType) {
        this.anno = anno;
        this.annoType = annoType;
    }


    /**
     * Returns a dynamic proxy for an annotation mirror.
     */
    public static <A extends Annotation> A generateAnnotation(
            Attribute.Compound anno, Class<A> annoType) {
        AnnotationProxyMaker apm = new AnnotationProxyMaker(anno, annoType);
        return annoType.cast(apm.generateAnnotation());
    }


    /**
     * Returns a dynamic proxy for an annotation mirror.
     */
    private Annotation generateAnnotation() {
        return AnnotationParser.annotationForMap(annoType,
                                                 getAllReflectedValues());
    }

    /**
     * Returns a map from element names to their values in "dynamic
     * proxy return form".  Includes all elements, whether explicit or
     * defaulted.
     */
    private Map<String, Object> getAllReflectedValues() {
        Map<String, Object> res = new LinkedHashMap<>();

        for (Map.Entry<MethodSymbol, Attribute> entry :
                                                  getAllValues().entrySet()) {
            MethodSymbol meth = entry.getKey();
            Object value = generateValue(meth, entry.getValue());
            if (value != null) {
                res.put(meth.name.toString(), value);
            } else {
                // Ignore this element.  May (properly) lead to
                // IncompleteAnnotationException somewhere down the line.
            }
        }
        return res;
    }

    /**
     * Returns a map from element symbols to their values.
     * Includes all elements, whether explicit or defaulted.
     */
    private Map<MethodSymbol, Attribute> getAllValues() {
        Map<MethodSymbol, Attribute> res = new LinkedHashMap<>();

        // First find the default values.
        ClassSymbol sym = (ClassSymbol) anno.type.tsym;
        for (Symbol s : sym.members().getSymbols(NON_RECURSIVE)) {
            if (s.kind == MTH) {
                MethodSymbol m = (MethodSymbol) s;
                Attribute def = m.getDefaultValue();
                if (def != null)
                    res.put(m, def);
            }
        }
        // Next find the explicit values, possibly overriding defaults.
        for (Pair<MethodSymbol, Attribute> p : anno.values)
            res.put(p.fst, p.snd);
        return res;
    }

    /**
     * Converts an element value to its "dynamic proxy return form".
     * Returns an exception proxy on some errors, but may return null if
     * a useful exception cannot or should not be generated at this point.
     */
    private Object generateValue(MethodSymbol meth, Attribute attr) {
        ValueVisitor vv = new ValueVisitor(meth);
        return vv.getValue(attr);
    }


    private class ValueVisitor implements Attribute.Visitor {

        private MethodSymbol meth;      // annotation element being visited
        private Class<?> returnClass;   // return type of annotation element
        private Object value;           // value in "dynamic proxy return form"

        ValueVisitor(MethodSymbol meth) {
            this.meth = meth;
        }

        Object getValue(Attribute attr) {
            Method method;              // runtime method of annotation element
            try {
                method = annoType.getMethod(meth.name.toString());
            } catch (NoSuchMethodException e) {
                return null;
            }
            returnClass = method.getReturnType();
            attr.accept(this);
            if (!(value instanceof ExceptionProxy) &&
                !AnnotationType.invocationHandlerReturnType(returnClass)
                                                        .isInstance(value)) {
                typeMismatch(method, attr);
            }
            return value;
        }


        public void visitConstant(Attribute.Constant c) {
            value = c.getValue();
        }

        public void visitClass(Attribute.Class c) {
            value = new MirroredTypeExceptionProxy(c.classType);
        }

        public void visitArray(Attribute.Array a) {
            Name elemName = ((ArrayType) a.type).elemtype.tsym.getQualifiedName();

            if (elemName.equals(elemName.table.names.java_lang_Class)) {   // Class[]
                // Construct a proxy for a MirroredTypesException
                ListBuffer<TypeMirror> elems = new ListBuffer<>();
                for (Attribute value : a.values) {
                    Type elem = ((Attribute.Class) value).classType;
                    elems.append(elem);
                }
                value = new MirroredTypesExceptionProxy(elems.toList());

            } else {
                int len = a.values.length;
                Class<?> returnClassSaved = returnClass;
                returnClass = returnClass.getComponentType();
                try {
                    Object res = Array.newInstance(returnClass, len);
                    for (int i = 0; i < len; i++) {
                        a.values[i].accept(this);
                        if (value == null || value instanceof ExceptionProxy) {
                            return;
                        }
                        try {
                            Array.set(res, i, value);
                        } catch (IllegalArgumentException e) {
                            value = null;       // indicates a type mismatch
                            return;
                        }
                    }
                    value = res;
                } finally {
                    returnClass = returnClassSaved;
                }
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public void visitEnum(Attribute.Enum e) {
            if (returnClass.isEnum()) {
                String constName = e.value.toString();
                try {
                    value = Enum.valueOf((Class)returnClass, constName);
                } catch (IllegalArgumentException ex) {
                    value = new EnumConstantNotPresentExceptionProxy(
                                        (Class<Enum<?>>) returnClass, constName);
                }
            } else {
                value = null;   // indicates a type mismatch
            }
        }

        public void visitCompound(Attribute.Compound c) {
            try {
                Class<? extends Annotation> nested =
                    returnClass.asSubclass(Annotation.class);
                value = generateAnnotation(c, nested);
            } catch (ClassCastException ex) {
                value = null;   // indicates a type mismatch
            }
        }

        public void visitError(Attribute.Error e) {
            if (e instanceof Attribute.UnresolvedClass unresolvedClass)
                value = new MirroredTypeExceptionProxy(unresolvedClass.classType);
            else
                value = null;       // indicates a type mismatch
        }


        /**
         * Sets "value" to an ExceptionProxy indicating a type mismatch.
         */
        private void typeMismatch(Method method, final Attribute attr) {
            class AnnotationTypeMismatchExceptionProxy extends ExceptionProxy {
                static final long serialVersionUID = 269;
                transient final Method method;
                AnnotationTypeMismatchExceptionProxy(Method method) {
                    this.method = method;
                }
                public String toString() {
                    return "<error>";   // eg:  @Anno(value=<error>)
                }
                protected RuntimeException generateException() {
                    return new AnnotationTypeMismatchException(method,
                                attr.type.toString());
                }
            }
            value = new AnnotationTypeMismatchExceptionProxy(method);
        }
    }


    /**
     * ExceptionProxy for MirroredTypeException.
     * The toString, hashCode, and equals methods forward to the underlying
     * type.
     */
    private static final class MirroredTypeExceptionProxy extends ExceptionProxy {
        static final long serialVersionUID = 269;

        private transient TypeMirror type;
        private final String typeString;

        MirroredTypeExceptionProxy(TypeMirror t) {
            type = t;
            typeString = t.toString();
        }

        public String toString() {
            return typeString + ".class";
        }

        public int hashCode() {
            return (type != null ? type : typeString).hashCode();
        }

        public boolean equals(Object obj) {
            return type != null &&
                   obj instanceof MirroredTypeExceptionProxy proxy &&
                   type.equals(proxy.type);
        }

        protected RuntimeException generateException() {
            return new MirroredTypeException(type);
        }

        // Explicitly set all transient fields.
        private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
            s.defaultReadObject();
            type = null;
        }
    }


    /**
     * ExceptionProxy for MirroredTypesException.
     * The toString, hashCode, and equals methods forward to the underlying
     * types.
     */
    private static final class MirroredTypesExceptionProxy extends ExceptionProxy {
        static final long serialVersionUID = 269;

        private transient List<TypeMirror> types;
        private final String typeStrings;

        MirroredTypesExceptionProxy(List<TypeMirror> ts) {
            types = ts;
            typeStrings = ts.toString();
        }

        public String toString() {
            return types.stream()
                .map(t -> t.toString() + ".class")
                .collect(Collectors.joining(", ", "{", "}"));
        }

        public int hashCode() {
            return (types != null ? types : typeStrings).hashCode();
        }

        public boolean equals(Object obj) {
            return types != null &&
                   obj instanceof MirroredTypesExceptionProxy proxy &&
                   types.equals(proxy.types);
        }

        protected RuntimeException generateException() {
            return new MirroredTypesException(types);
        }

        // Explicitly set all transient fields.
        private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
            s.defaultReadObject();
            types = null;
        }
    }
}
