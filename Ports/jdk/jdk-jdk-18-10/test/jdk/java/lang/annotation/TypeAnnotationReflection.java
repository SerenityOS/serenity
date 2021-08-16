/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004698 8007073 8022343 8054304 8057804 8058595
 * @summary Unit test for type annotations
 */

import java.util.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.io.Serializable;

public class TypeAnnotationReflection {
    public static void main(String[] args) throws Exception {
        testSuper();
        testInterfaces();
        testReturnType();
        testNested();
        testArray();
        testRunException();
        testClassTypeVarBounds();
        testMethodTypeVarBounds();
        testFields();
        testClassTypeVar();
        testMethodTypeVar();
        testParameterizedType();
        testNestedParameterizedType();
        testWildcardType();
        testParameterTypes();
        testParameterType();
    }

    private static void check(boolean b) {
        if (!b)
            throw new RuntimeException();
    }

    private static void testSuper() throws Exception {
        check(Object.class.getAnnotatedSuperclass() == null);
        check(Class.class.getAnnotatedSuperclass().getAnnotations().length == 0);

        AnnotatedType a;
        a = TestClassArray.class.getAnnotatedSuperclass();
        Annotation[] annos = a.getAnnotations();
        check(annos.length == 2);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(annos[1].annotationType().equals(TypeAnno2.class));
        check(((TypeAnno)annos[0]).value().equals("extends"));
        check(((TypeAnno2)annos[1]).value().equals("extends2"));
    }

    private static void testInterfaces() throws Exception {
        AnnotatedType[] as;
        as = TestClassArray.class.getAnnotatedInterfaces();
        check(as.length == 3);
        check(as[1].getAnnotations().length == 0);

        Annotation[] annos;
        annos = as[0].getAnnotations();
        check(annos.length == 2);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(annos[1].annotationType().equals(TypeAnno2.class));
        check(((TypeAnno)annos[0]).value().equals("implements serializable"));
        check(((TypeAnno2)annos[1]).value().equals("implements2 serializable"));

        annos = as[2].getAnnotations();
        check(annos.length == 2);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(annos[1].annotationType().equals(TypeAnno2.class));
        check(((TypeAnno)annos[0]).value().equals("implements cloneable"));
        check(((TypeAnno2)annos[1]).value().equals("implements2 cloneable"));
    }

    private static void testReturnType() throws Exception {
        Method m = TestClassArray.class.getDeclaredMethod("foo", (Class<?>[])null);
        Annotation[] annos = m.getAnnotatedReturnType().getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(((TypeAnno)annos[0]).value().equals("return1"));
    }

    private static void testNested() throws Exception {
        Method m = TestClassNested.class.getDeclaredMethod("foo", (Class<?>[])null);
        Annotation[] annos = m.getAnnotatedReturnType().getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(((TypeAnno)annos[0]).value().equals("array"));

        AnnotatedType t = m.getAnnotatedReturnType();
        t = ((AnnotatedArrayType)t).getAnnotatedGenericComponentType();
        annos = t.getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(((TypeAnno)annos[0]).value().equals("Inner"));
    }

    private static void testArray() throws Exception {
        Method m = TestClassArray.class.getDeclaredMethod("foo", (Class<?>[])null);
        AnnotatedArrayType t = (AnnotatedArrayType) m.getAnnotatedReturnType();
        Annotation[] annos = t.getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(((TypeAnno)annos[0]).value().equals("return1"));

        t = (AnnotatedArrayType)t.getAnnotatedGenericComponentType();
        annos = t.getAnnotations();
        check(annos.length == 0);

        t = (AnnotatedArrayType)t.getAnnotatedGenericComponentType();
        annos = t.getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(((TypeAnno)annos[0]).value().equals("return3"));

        AnnotatedType tt = t.getAnnotatedGenericComponentType();
        check(!(tt instanceof AnnotatedArrayType));
        annos = tt.getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(((TypeAnno)annos[0]).value().equals("return4"));
    }

    private static void testRunException() throws Exception {
        Method m = TestClassException.class.getDeclaredMethod("foo", (Class<?>[])null);
        AnnotatedType[] ts = m.getAnnotatedExceptionTypes();
        check(ts.length == 3);

        AnnotatedType t;
        Annotation[] annos;
        t = ts[0];
        annos = t.getAnnotations();
        check(annos.length == 2);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(annos[1].annotationType().equals(TypeAnno2.class));
        check(((TypeAnno)annos[0]).value().equals("RE"));
        check(((TypeAnno2)annos[1]).value().equals("RE2"));

        t = ts[1];
        annos = t.getAnnotations();
        check(annos.length == 0);

        t = ts[2];
        annos = t.getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(((TypeAnno)annos[0]).value().equals("AIOOBE"));
    }

    private static void testClassTypeVarBounds() throws Exception {
        Method m = TestClassTypeVarAndField.class.getDeclaredMethod("foo", (Class<?>[])null);
        AnnotatedType ret = m.getAnnotatedReturnType();
        Annotation[] annos = ret.getAnnotations();
        check(annos.length == 2);

        AnnotatedType[] annotatedBounds = ((AnnotatedTypeVariable)ret).getAnnotatedBounds();
        check(annotatedBounds.length == 2);

        annos = annotatedBounds[0].getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(((TypeAnno)annos[0]).value().equals("Object1"));

        annos = annotatedBounds[1].getAnnotations();
        check(annos.length == 2);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(annos[1].annotationType().equals(TypeAnno2.class));
        check(((TypeAnno)annos[0]).value().equals("Runnable1"));
        check(((TypeAnno2)annos[1]).value().equals("Runnable2"));
    }

    private static void testMethodTypeVarBounds() throws Exception {
        Method m2 = TestClassTypeVarAndField.class.getDeclaredMethod("foo2", (Class<?>[])null);
        AnnotatedType ret2 = m2.getAnnotatedReturnType();
        AnnotatedType[] annotatedBounds2 = ((AnnotatedTypeVariable)ret2).getAnnotatedBounds();
        check(annotatedBounds2.length == 1);

        Annotation[] annos = annotatedBounds2[0].getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(((TypeAnno)annos[0]).value().equals("M Runnable"));

        // Check that AnnotatedTypeVariable.getAnnotatedBounds() returns jlO for a naked
        // type variable (i.e no bounds, no annotations)
        Method m4 = TestClassTypeVarAndField.class.getDeclaredMethod("foo4", (Class<?>[])null);
        AnnotatedType ret4 = m4.getAnnotatedReturnType();
        AnnotatedType[] annotatedBounds4 = ((AnnotatedTypeVariable)ret4).getAnnotatedBounds();
        check(annotatedBounds4.length == 1);

        annos = annotatedBounds4[0].getAnnotations();
        check(annos.length == 0);
        check(annotatedBounds4[0].getType().equals(Object.class));
    }

    private static void testFields() throws Exception {
        Field f1 = TestClassTypeVarAndField.class.getDeclaredField("field1");
        AnnotatedType at;
        Annotation[] annos;

        at = f1.getAnnotatedType();
        annos = at.getAnnotations();
        check(annos.length == 2);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(annos[1].annotationType().equals(TypeAnno2.class));
        check(((TypeAnno)annos[0]).value().equals("T1 field"));
        check(((TypeAnno2)annos[1]).value().equals("T2 field"));

        Field f2 = TestClassTypeVarAndField.class.getDeclaredField("field2");
        at = f2.getAnnotatedType();
        annos = at.getAnnotations();
        check(annos.length == 0);

        Field f3 = TestClassTypeVarAndField.class.getDeclaredField("field3");
        at = f3.getAnnotatedType();
        annos = at.getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(((TypeAnno)annos[0]).value().equals("Object field"));
    }

    private static void testClassTypeVar() throws Exception {
        TypeVariable[] typeVars = TestClassTypeVarAndField.class.getTypeParameters();
        Annotation[] annos;
        check(typeVars.length == 3);

        // First TypeVar
        AnnotatedType[] annotatedBounds = typeVars[0].getAnnotatedBounds();
        check(annotatedBounds.length == 2);

        annos = annotatedBounds[0].getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(((TypeAnno)annos[0]).value().equals("Object1"));

        annos = annotatedBounds[1].getAnnotations();
        check(annos.length == 2);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(annos[1].annotationType().equals(TypeAnno2.class));
        check(((TypeAnno)annos[0]).value().equals("Runnable1"));
        check(((TypeAnno2)annos[1]).value().equals("Runnable2"));

        // second TypeVar regular anno
        Annotation[] regularAnnos = typeVars[1].getAnnotations();
        check(regularAnnos.length == 1);
        check(typeVars[1].getAnnotation(TypeAnno.class).value().equals("EE"));

        // second TypeVar
        annotatedBounds = typeVars[1].getAnnotatedBounds();
        check(annotatedBounds.length == 1);

        annos = annotatedBounds[0].getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno2.class));
        check(((TypeAnno2)annos[0]).value().equals("EEBound"));

        // third Typevar V declared without explicit bounds should see jlO as its bound.
        annotatedBounds = typeVars[2].getAnnotatedBounds();
        check(annotatedBounds.length == 1);

        annos = annotatedBounds[0].getAnnotations();
        check(annos.length == 0);
        check(annotatedBounds[0].getType().equals(Object.class));
    }

    private static void testMethodTypeVar() throws Exception {
        Method m2 = TestClassTypeVarAndField.class.getDeclaredMethod("foo2", (Class<?>[])null);
        TypeVariable[] t = m2.getTypeParameters();
        check(t.length == 1);
        Annotation[] annos = t[0].getAnnotations();
        check(annos.length == 0);

        AnnotatedType[] annotatedBounds2 = t[0].getAnnotatedBounds();
        check(annotatedBounds2.length == 1);

        annos = annotatedBounds2[0].getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(((TypeAnno)annos[0]).value().equals("M Runnable"));

        // Second method
        m2 = TestClassTypeVarAndField.class.getDeclaredMethod("foo3", (Class<?>[])null);
        t = m2.getTypeParameters();
        check(t.length == 2);
        annos = t[0].getAnnotations();
        check(annos.length == 1);
        check(annos[0].annotationType().equals(TypeAnno.class));
        check(((TypeAnno)annos[0]).value().equals("K"));

        annotatedBounds2 = t[0].getAnnotatedBounds();
        check(annotatedBounds2.length == 1);

        annos = annotatedBounds2[0].getAnnotations();
        check(annos.length == 0);

        // for the naked type variable L of foo3, we should see jlO as its bound.
        annotatedBounds2 = t[1].getAnnotatedBounds();
        check(annotatedBounds2.length == 1);
        check(annotatedBounds2[0].getType().equals(Object.class));

        annos = annotatedBounds2[0].getAnnotations();
        check(annos.length == 0);
    }

    private static void testParameterizedType() {
        // Base
        AnnotatedType[] as;
        as = TestParameterizedType.class.getAnnotatedInterfaces();
        check(as.length == 1);
        check(as[0].getAnnotations().length == 1);
        check(as[0].getAnnotation(TypeAnno.class).value().equals("M"));

        Annotation[] annos;
        as = ((AnnotatedParameterizedType)as[0]).getAnnotatedActualTypeArguments();
        check(as.length == 2);
        annos = as[0].getAnnotations();
        check(annos.length == 1);
        check(as[0].getAnnotation(TypeAnno.class).value().equals("S"));
        check(as[0].getAnnotation(TypeAnno2.class) == null);

        annos = as[1].getAnnotations();
        check(annos.length == 2);
        check(((TypeAnno)annos[0]).value().equals("I"));
        check(as[1].getAnnotation(TypeAnno2.class).value().equals("I2"));
    }

    private static void testNestedParameterizedType() throws Exception {
        Method m = TestParameterizedType.class.getDeclaredMethod("foo2", (Class<?>[])null);
        AnnotatedType ret = m.getAnnotatedReturnType();
        Annotation[] annos;
        annos = ret.getAnnotations();
        check(annos.length == 1);
        check(((TypeAnno)annos[0]).value().equals("I"));

        AnnotatedType[] args = ((AnnotatedParameterizedType)ret).getAnnotatedActualTypeArguments();
        check(args.length == 1);
        annos = args[0].getAnnotations();
        check(annos.length == 2);
        check(((TypeAnno)annos[0]).value().equals("I1"));
        check(args[0].getAnnotation(TypeAnno2.class).value().equals("I2"));

        // check type args
        Field f = TestParameterizedType.class.getDeclaredField("theField");
        AnnotatedParameterizedType fType = (AnnotatedParameterizedType)f.getAnnotatedType();
        args = fType.getAnnotatedActualTypeArguments();
        check(args.length == 1);
        annos = args[0].getAnnotations();
        check(annos.length == 1);
        check(((TypeAnno2)annos[0]).value().equals("Map Arg"));
        check(args[0].getAnnotation(TypeAnno2.class).value().equals("Map Arg"));

        // check outer type type args
        fType = (AnnotatedParameterizedType)fType.getAnnotatedOwnerType();
        args = fType.getAnnotatedActualTypeArguments();
        check(args.length == 1);
        annos = args[0].getAnnotations();
        check(annos.length == 1);
        check(((TypeAnno2)annos[0]).value().equals("String Arg"));
        check(args[0].getAnnotation(TypeAnno2.class).value().equals("String Arg"));

        // check outer type normal type annotations
        annos = fType.getAnnotations();
        check(annos.length == 1);
        check(((TypeAnno)annos[0]).value().equals("FieldOuter"));
        check(fType.getAnnotation(TypeAnno.class).value().equals("FieldOuter"));
    }

    private static void testWildcardType() throws Exception {
        Method m = TestWildcardType.class.getDeclaredMethod("foo", (Class<?>[])null);
        AnnotatedType ret = m.getAnnotatedReturnType();
        AnnotatedType[] t;
        t = ((AnnotatedParameterizedType)ret).getAnnotatedActualTypeArguments();
        check(t.length == 1);
        ret = t[0];

        Field f = TestWildcardType.class.getDeclaredField("f1");
        AnnotatedWildcardType w = (AnnotatedWildcardType)((AnnotatedParameterizedType)f
            .getAnnotatedType()).getAnnotatedActualTypeArguments()[0];
        t = w.getAnnotatedLowerBounds();
        check(t.length == 0);
        t = w.getAnnotatedUpperBounds();
        check(t.length == 1);
        Annotation[] annos;
        annos = t[0].getAnnotations();
        check(annos.length == 1);
        check(((TypeAnno)annos[0]).value().equals("2"));

        f = TestWildcardType.class.getDeclaredField("f2");
        w = (AnnotatedWildcardType)((AnnotatedParameterizedType)f
            .getAnnotatedType()).getAnnotatedActualTypeArguments()[0];
        t = w.getAnnotatedUpperBounds();
        check(t.length == 1);
        check(t[0].getType().equals(Object.class));
        annos = t[0].getAnnotations();
        check(annos.length == 0);
        t = w.getAnnotatedLowerBounds();
        check(t.length == 1);

        // for an unbounded wildcard, we should see jlO as its upperbound and null type as its lower bound.
        f = TestWildcardType.class.getDeclaredField("f3");
        w = (AnnotatedWildcardType)((AnnotatedParameterizedType)f
            .getAnnotatedType()).getAnnotatedActualTypeArguments()[0];
        t = w.getAnnotatedUpperBounds();
        check(t.length == 1);
        check(t[0].getType().equals(Object.class));
        annos = t[0].getAnnotations();
        check(annos.length == 0);
        t = w.getAnnotatedLowerBounds();
        check(t.length == 0);
    }

    private static void testParameterTypes() throws Exception {
        // NO PARAMS
        Method m = Params.class.getDeclaredMethod("noParams", (Class<?>[])null);
        AnnotatedType[] t = m.getAnnotatedParameterTypes();
        check(t.length == 0);

        // ONLY ANNOTATED PARAM TYPES
        Class[] argsArr = {String.class, String.class, String.class};
        m = Params.class.getDeclaredMethod("onlyAnnotated", (Class<?>[])argsArr);
        t = m.getAnnotatedParameterTypes();
        check(t.length == 3);

        check(t[0].getAnnotations().length == 1);
        check(t[0].getAnnotation(TypeAnno.class) != null);
        check(t[0].getAnnotationsByType(TypeAnno.class)[0].value().equals("1"));

        check(t[1].getAnnotations().length == 1);
        check(t[1].getAnnotation(TypeAnno.class) != null);
        check(t[1].getAnnotationsByType(TypeAnno.class)[0].value().equals("2"));

        check(t[2].getAnnotations().length == 2);
        check(t[2].getAnnotations()[0].annotationType().equals(TypeAnno.class));
        check(t[2].getAnnotation(TypeAnno.class) != null);
        check(t[2].getAnnotation(TypeAnno2.class) != null);
        check(t[2].getAnnotationsByType(TypeAnno.class)[0].value().equals("3a"));
        check(t[2].getAnnotationsByType(TypeAnno2.class)[0].value().equals("3b"));

        // MIXED ANNOTATED PARAM TYPES
        m = Params.class.getDeclaredMethod("mixed", (Class<?>[])argsArr);
        t = m.getAnnotatedParameterTypes();
        check(t.length == 3);

        check(t[0].getAnnotations().length == 1);
        check(t[0].getAnnotation(TypeAnno.class) != null);
        check(t[0].getAnnotationsByType(TypeAnno.class)[0].value().equals("1"));

        check(t[1].getAnnotations().length == 0);
        check(t[1].getAnnotation(TypeAnno.class) == null);
        check(t[1].getAnnotation(TypeAnno2.class) == null);

        check(t[2].getAnnotations().length == 2);
        check(t[2].getAnnotations()[0].annotationType().equals(TypeAnno.class));
        check(t[2].getAnnotation(TypeAnno.class) != null);
        check(t[2].getAnnotation(TypeAnno2.class) != null);
        check(t[2].getAnnotationsByType(TypeAnno.class)[0].value().equals("3a"));
        check(t[2].getAnnotationsByType(TypeAnno2.class)[0].value().equals("3b"));

        // NO ANNOTATED PARAM TYPES
        m = Params.class.getDeclaredMethod("unAnnotated", (Class<?>[])argsArr);
        t = m.getAnnotatedParameterTypes();
        check(t.length == 3);

        check(t[0].getAnnotations().length == 0);
        check(t[0].getAnnotation(TypeAnno.class) == null);
        check(t[0].getAnnotation(TypeAnno2.class) == null);

        check(t[1].getAnnotations().length == 0);
        check(t[1].getAnnotation(TypeAnno.class) == null);
        check(t[1].getAnnotation(TypeAnno2.class) == null);

        check(t[2].getAnnotations().length == 0);
        check(t[2].getAnnotation(TypeAnno.class) == null);
        check(t[2].getAnnotation(TypeAnno2.class) == null);
    }

    private static void testParameterType() throws Exception {
        // NO PARAMS
        Method m = Params.class.getDeclaredMethod("noParams", (Class<?>[])null);
        Parameter[] p = m.getParameters();
        check(p.length == 0);

        // ONLY ANNOTATED PARAM TYPES
        Class[] argsArr = {String.class, String.class, String.class};
        m = Params.class.getDeclaredMethod("onlyAnnotated", (Class<?>[])argsArr);
        p = m.getParameters();
        check(p.length == 3);
        AnnotatedType t0 = p[0].getAnnotatedType();
        AnnotatedType t1 = p[1].getAnnotatedType();
        AnnotatedType t2 = p[2].getAnnotatedType();

        check(t0.getAnnotations().length == 1);
        check(t0.getAnnotation(TypeAnno.class) != null);
        check(t0.getAnnotationsByType(TypeAnno.class)[0].value().equals("1"));

        check(t1.getAnnotations().length == 1);
        check(t1.getAnnotation(TypeAnno.class) != null);
        check(t1.getAnnotationsByType(TypeAnno.class)[0].value().equals("2"));

        check(t2.getAnnotations().length == 2);
        check(t2.getAnnotations()[0].annotationType().equals(TypeAnno.class));
        check(t2.getAnnotation(TypeAnno.class) != null);
        check(t2.getAnnotation(TypeAnno2.class) != null);
        check(t2.getAnnotationsByType(TypeAnno.class)[0].value().equals("3a"));
        check(t2.getAnnotationsByType(TypeAnno2.class)[0].value().equals("3b"));

        // MIXED ANNOTATED PARAM TYPES
        m = Params.class.getDeclaredMethod("mixed", (Class<?>[])argsArr);
        p = m.getParameters();
        check(p.length == 3);

        t0 = p[0].getAnnotatedType();
        t1 = p[1].getAnnotatedType();
        t2 = p[2].getAnnotatedType();

        check(t0.getAnnotations().length == 1);
        check(t0.getAnnotation(TypeAnno.class) != null);
        check(t0.getAnnotationsByType(TypeAnno.class)[0].value().equals("1"));

        check(t1.getAnnotations().length == 0);
        check(t1.getAnnotation(TypeAnno.class) == null);
        check(t1.getAnnotation(TypeAnno2.class) == null);

        check(t2.getAnnotations().length == 2);
        check(t2.getAnnotations()[0].annotationType().equals(TypeAnno.class));
        check(t2.getAnnotation(TypeAnno.class) != null);
        check(t2.getAnnotation(TypeAnno2.class) != null);
        check(t2.getAnnotationsByType(TypeAnno.class)[0].value().equals("3a"));
        check(t2.getAnnotationsByType(TypeAnno2.class)[0].value().equals("3b"));

        // NO ANNOTATED PARAM TYPES
        m = Params.class.getDeclaredMethod("unAnnotated", (Class<?>[])argsArr);
        p = m.getParameters();
        check(p.length == 3);

        t0 = p[0].getAnnotatedType();
        t1 = p[1].getAnnotatedType();
        t2 = p[2].getAnnotatedType();

        check(t0.getAnnotations().length == 0);
        check(t0.getAnnotation(TypeAnno.class) == null);
        check(t0.getAnnotation(TypeAnno2.class) == null);

        check(t1.getAnnotations().length == 0);
        check(t1.getAnnotation(TypeAnno.class) == null);
        check(t1.getAnnotation(TypeAnno2.class) == null);

        check(t2.getAnnotations().length == 0);
        check(t2.getAnnotation(TypeAnno.class) == null);
        check(t2.getAnnotation(TypeAnno2.class) == null);
    }
}

class Params {
    public void noParams() {}
    public void onlyAnnotated(@TypeAnno("1") String s1, @TypeAnno("2") String s2, @TypeAnno("3a") @TypeAnno2("3b") String s3) {}
    public void mixed(@TypeAnno("1") String s1, String s2, @TypeAnno("3a") @TypeAnno2("3b") String s3) {}
    public void unAnnotated(String s1, String s2, String s3) {}
}

abstract class TestWildcardType {
    public <T> List<? super T> foo() { return null;}
    public Class<@TypeAnno("1") ? extends @TypeAnno("2") Annotation> f1;
    public Class<@TypeAnno("3") ? super @TypeAnno("4") Annotation> f2;
    public Class<@TypeAnno("5") ?> f3;
}

abstract class TestParameterizedType implements @TypeAnno("M") Map<@TypeAnno("S")String, @TypeAnno("I") @TypeAnno2("I2")Integer> {
    public ParameterizedOuter<String>.ParameterizedInner<Integer> foo() {return null;}
    public @TypeAnno("O") ParameterizedOuter<@TypeAnno("S1") @TypeAnno2("S2") String>.
            @TypeAnno("I") ParameterizedInner<@TypeAnno("I1") @TypeAnno2("I2")Integer> foo2() {
        return null;
    }

    public @TypeAnno("FieldOuter") ParameterizedOuter<@TypeAnno2("String Arg") String>.
            @TypeAnno("FieldInner")ParameterizedInner<@TypeAnno2("Map Arg")Map> theField;
}

class ParameterizedOuter <T> {
    class ParameterizedInner <U> {}
}

abstract class TestClassArray extends @TypeAnno("extends") @TypeAnno2("extends2") Object
    implements @TypeAnno("implements serializable") @TypeAnno2("implements2 serializable") Serializable,
    Readable,
    @TypeAnno("implements cloneable") @TypeAnno2("implements2 cloneable") Cloneable {
    public @TypeAnno("return4") Object @TypeAnno("return1") [][] @TypeAnno("return3")[] foo() { return null; }
}

abstract class TestClassNested {
    public @TypeAnno("Outer") Outer.@TypeAnno("Inner")Inner @TypeAnno("array")[] foo() { return null; }
}

class Outer {
    class Inner {
    }
}

abstract class TestClassException {
    public Object foo() throws @TypeAnno("RE") @TypeAnno2("RE2") RuntimeException,
                                                                 NullPointerException,
                                             @TypeAnno("AIOOBE") ArrayIndexOutOfBoundsException {
        return null;
    }
}

abstract class TestClassTypeVarAndField <T extends @TypeAnno("Object1") Object
                                          & @TypeAnno("Runnable1") @TypeAnno2("Runnable2") Runnable,
                                        @TypeAnno("EE")EE extends @TypeAnno2("EEBound") Runnable, V > {
    @TypeAnno("T1 field") @TypeAnno2("T2 field") T field1;
    T field2;
    @TypeAnno("Object field") Object field3;

    public @TypeAnno("t1") @TypeAnno2("t2") T foo(){ return null; }
    public <M extends @TypeAnno("M Runnable") Runnable> M foo2() {return null;}
    public <@TypeAnno("K") K extends Cloneable, L> K foo3() {return null;}
    public <L> L foo4() {return null;}
}

@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@interface TypeAnno {
    String value();
}

@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@interface TypeAnno2 {
    String value();
}
