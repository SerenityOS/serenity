/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 7007432 7006109
 * @summary Test generic types well-formedness
 * @author mcimadamore
 * @library /tools/lib/types
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.tree
 * @build TypeHarness
 * @run main GenericTypeWellFormednessTest
 */

import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import java.lang.reflect.Array;

/**
 * Check parameterized type well-formedness. This test executes a number of checks
 * in order to establish as to whether an instantiation of a generic type conforms
 * to the generic class' declared bounds.
 */
public class GenericTypeWellFormednessTest extends TypeHarness {

    static int executedCount = 0;
    static int ignoredCount = 0;

    InstantiableType[] rows;
    Type[] columns;

    static class InstantiableType {
        protected Type type;

        public InstantiableType(Type type) {
            this.type = type;
        }

        Type inst(Type clazz) {
            return type;
        }
    }

    enum Result {
        /* generic type is well-formed w.r.t. declared bounds */
        OK(true),
        /* generic type is not well-formed w.r.t. declared bounds */
        FAIL(false),
        /* generic type is not well-formed w.r.t. declared bounds according to the JLS 3rd,
         * but javac allows it (spec for generic type well-formedness is overly restrictive)
         * See regression test test/tools/generics/wildcards/T5097548.java
         */
        IGNORE(false);

        boolean value;

        Result(boolean value) {
            this.value = value;
        }
    }

    static final Result T = Result.OK;
    static final Result F = Result.FAIL;
    static final Result I = Result.IGNORE;

    /*is a type in 'rows' a valid instantiation for the generic class in 'col' ? */
    Result[][] isValidInstantiation = {
                     //Foo<X>, Foo<X ext Object>, Foo<X ext Number>, Foo<X ext Foo<X>>, Foo<X ext Foo<+X>>, Foo<X ext Foo<-X>>, Foo<X ext Foo<?>>
    /*Foo<Object>*/  { T     , T                , F                , F                , F                 , F                 , F },
    /*Foo<Number>*/  { T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<Integer>*/ { T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<Double>*/  { T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<String>*/  { T     , T                , F                , F                , F                 , F                 , F },
    /*Foo<X1>*/      { T     , T                , F                , F                , F                 , F                 , F },
    /*Foo<X2>*/      { T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<X3>*/      { T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<X4>*/      { T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<X5>*/      { T     , T                , F                , F                , F                 , F                 , F },
    /*Foo<X6>*/      { T     , T                , F                , T                , T                 , T                 , T },
    /*Foo<+Object>*/ { T     , T                , I                , I                , I                 , I                 , I },
    /*Foo<+Number>*/ { T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<+Integer>*/{ T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<+Double>*/ { T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<+String>*/ { T     , T                , F                , F                , F                 , F                 , F },
    /*Foo<+X1>*/     { T     , T                , F                , F                , F                 , F                 , F },
    /*Foo<+X2>*/     { T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<+X3>*/     { T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<+X4>*/     { T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<+X5>*/     { T     , T                , F                , F                , F                 , F                 , F },
    /*Foo<+X6>*/     { T     , T                , F                , T                , T                 , I                 , T },
    /*Foo<-Object>*/ { T     , T                , F                , F                , F                 , F                 , F },
    /*Foo<-Number>*/ { T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<-Integer>*/{ T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<-Double>*/ { T     , T                , T                , F                , F                 , F                 , F },
    /*Foo<-String>*/ { T     , T                , F                , F                , F                 , F                 , F },
    /*Foo<-X1>*/     { T     , T                , I                , I                , I                 , I                 , I },
    /*Foo<-X2>*/     { T     , T                , I                , F                , F                 , F                 , F },
    /*Foo<-X3>*/     { T     , T                , I                , F                , F                 , F                 , F },
    /*Foo<-X4>*/     { T     , T                , I                , F                , F                 , F                 , F },
    /*Foo<-X5>*/     { T     , T                , I                , F                , F                 , F                 , F },
    /*Foo<-X6>*/     { T     , T                , F                , T                , I                 , I                 , T },
    /*Foo<?>*/       { T     , T                , T                , T                , T                 , T                 , T }};

    GenericTypeWellFormednessTest() {
        InstantiableType[] basicTypes = {
            new InstantiableType(predef.objectType),
            new InstantiableType(NumberType()),
            new InstantiableType(box(predef.intType)),
            new InstantiableType(box(predef.doubleType)),
            new InstantiableType(predef.stringType) };

        InstantiableType[] typeVars = new InstantiableType[basicTypes.length + 1];
        for (int i = 0 ; i < basicTypes.length ; i++) {
           typeVars[i] = new InstantiableType(fac.TypeVariable(basicTypes[i].type));
        }
        typeVars[typeVars.length - 1] = new InstantiableType(null) {
            Type inst(Type clazz) {
                TypeVar tvar = fac.TypeVariable();
                tvar.setUpperBound( subst(clazz, Mapping(clazz.getTypeArguments().head, tvar)) );
                return tvar;
            }
        };

        InstantiableType[] typeArgs = join(InstantiableType.class, basicTypes, typeVars);

        InstantiableType[] invariantTypes = new InstantiableType[typeArgs.length];
        for (int i = 0 ; i < typeArgs.length ; i++) {
           final InstantiableType type1 = typeArgs[i];
           invariantTypes[i] = new InstantiableType(typeArgs[i].type) {
               Type inst(Type clazz) {
                   return subst(clazz, Mapping(clazz.getTypeArguments().head, type1.inst(clazz)));
               }
            };
        }

        InstantiableType[] covariantTypes = new InstantiableType[typeArgs.length];
        for (int i = 0 ; i < typeArgs.length ; i++) {
           final InstantiableType type1 = typeArgs[i];
           covariantTypes[i] = new InstantiableType(null) {
               Type inst(Type clazz) {
                   Type t = fac.Wildcard(BoundKind.EXTENDS, type1.inst(clazz));
                   return subst(clazz, Mapping(clazz.getTypeArguments().head, t));
               }
            };
        }

        InstantiableType[] contravariantTypes = new InstantiableType[typeArgs.length];
        for (int i = 0 ; i < typeArgs.length ; i++) {
           final InstantiableType type1 = typeArgs[i];
           contravariantTypes[i] = new InstantiableType(null) {
               Type inst(Type clazz) {
                   Type t = fac.Wildcard(BoundKind.SUPER, type1.inst(clazz));
                   return subst(clazz, Mapping(clazz.getTypeArguments().head, t));
               }
            };
        }

        InstantiableType[] bivariantTypes = {
            new InstantiableType(fac.Wildcard(BoundKind.UNBOUND, predef.objectType)) {
               Type inst(Type clazz) {
                   return subst(clazz, Mapping(clazz.getTypeArguments().head, type));
               }
            }
        };

        rows = join(InstantiableType.class, invariantTypes, covariantTypes, contravariantTypes, bivariantTypes);

        Type tv1 = fac.TypeVariable();
        Type decl1 = fac.Class(tv1);

        Type tv2 = fac.TypeVariable(predef.objectType);
        Type decl2 = fac.Class(tv2);

        Type tv3 = fac.TypeVariable(NumberType());
        Type decl3 = fac.Class(tv3);

        TypeVar tv4 = fac.TypeVariable();
        Type decl4 = fac.Class(tv4);
        tv4.setUpperBound(decl4);
        tv4.tsym.name = predef.exceptionType.tsym.name;

        TypeVar tv5 = fac.TypeVariable();
        Type decl5 = fac.Class(tv5);
        tv5.setUpperBound( subst(decl5, Mapping(tv5, fac.Wildcard(BoundKind.EXTENDS, tv5))) );

        TypeVar tv6 = fac.TypeVariable();
        Type decl6 = fac.Class(tv6);
        tv6.setUpperBound( subst(decl6, Mapping(tv6, fac.Wildcard(BoundKind.SUPER, tv6))) );

        TypeVar tv7 = fac.TypeVariable();
        Type decl7 = fac.Class(tv7);
        tv7.setUpperBound( subst(decl7, Mapping(tv7, fac.Wildcard(BoundKind.UNBOUND, predef.objectType))) );

        columns = new Type[] {
            decl1, decl2, decl3, decl4, decl5, decl6, decl7
        };
    }

    void test() {
        for (int i = 0; i < rows.length ; i++) {
            for (int j = 0; j < columns.length ; j++) {
                Type decl = columns[j];
                Type inst = rows[i].inst(decl);
                if (isValidInstantiation[i][j] != Result.IGNORE) {
                    executedCount++;
                    assertValidGenericType(inst, isValidInstantiation[i][j].value);
                } else {
                    ignoredCount++;
                }
            }
        }
    }

    Type NumberType() {
        Symbol s = box(predef.intType).tsym;
        s.complete();
        return ((ClassType)s.type).supertype_field;
    }

    @SuppressWarnings("unchecked")
    <T> T[] join(Class<T> type, T[]... args) {
        int totalLength = 0;
        for (T[] arr : args) {
            totalLength += arr.length;
        }
        T[] new_arr = (T[])Array.newInstance(type, totalLength);
        int idx = 0;
        for (T[] arr : args) {
            System.arraycopy(arr, 0, new_arr, idx, arr.length);
            idx += arr.length;
        }
        return new_arr;
    }

    public static void main(String[] args) {
        new GenericTypeWellFormednessTest().test();
        System.out.println("Executed checks : " + executedCount);
        System.out.println("Ignored checks : " + ignoredCount);
    }
}
