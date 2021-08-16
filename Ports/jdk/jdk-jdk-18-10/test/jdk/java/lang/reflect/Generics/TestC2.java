/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
* @bug 4891872
* @summary Some tests for the generic core reflection api.
* @author Gilad Bracha
* @compile TestC2.java
* @run main/othervm -ea TestC2
*/


import java.lang.reflect.*;


abstract class C0<T> {

    public T ft;
    public C0<T> fc1t;
    public C0 fc1;

    public C0(){}
    public C0(T t) {}

    public abstract  C0<T> mc1t(T t, C0<T> c1t, C0 c1);

    public abstract C0 mc1();

    public abstract T mt(T t);

}

interface I1<X1, X2> extends I3 {

    X1 foo(X2 x2);
}

interface I2<E1, E2 extends Throwable, E3> {


    E1 bar(E3 e3) throws E2;

}

interface I3 {


}


abstract class C2<T1 extends C2<T1, T2, T3>, T2 extends C0<T2>,
                                                        T3 extends Throwable>
    extends C0<T1>
    implements I1<T1, T2>, I2<T1, T3, T2>, I3
{

    public T1 ft;
    public C0<String> fc1t;
    public C0 fc1;
    public int fi;

    public C2(T2 t2) {}
    public <T> C2(T t) {}
    public <T1, T2, T3, T4> C2(T1 t1, T2 t2, T4 t4) {}
    public C2() throws T3 {}

    public abstract <T>  C0<T> mc1t(T3 t3, C0<T> c1t, C0 c1);

    public abstract <E, R> C0 mc1(E e);

    public abstract T1 mt(T2 t);

}

public class TestC2 {

    static Class<C2> cls = C2.class;


    public static void main(String[] args) throws Throwable {
        testSuperclass();
        testSuperInterfaces();
        testTypeParameters();
        testMethods();
        testConstructors();
        testFields();
    }

    static void testSuperclass() {

        System.out.println("testing superclass");
        Type sc = cls.getGenericSuperclass();
        assert
            sc instanceof ParameterizedType :
            "Superclass of C2 should be a parameterized type";
        ParameterizedType psc = (ParameterizedType) sc;
        assert
            ((psc.getRawType() == C0.class) ) :
            "The raw generic superclass of C2 should be C0";

        Type[] tas = psc.getActualTypeArguments();
        assert
            tas.length == 1 :
            "Superclass of C2 should have one type argument";

        Type t = tas[0];

        assert
            t instanceof TypeVariable :
            "Type argument to superclass of C2 should be a type variable";

        TypeVariable tv = (TypeVariable) t;
        assert
            tv.getName().equals("T1") :
            "Name of type argument to superclass of C2 should be T1";
        Type[] bs = tv.getBounds();
        assert
            bs.length == 1 :
            "T1 has one bound (superclass)";
        t = bs[0];
        assert
            t instanceof ParameterizedType :
            "Bound of C0 should be a parameterized type";
        ParameterizedType pt = (ParameterizedType) t;

        assert
            ((pt.getRawType() == C2.class) ) :
            "The raw bound of T1 should be C2";

        tas = pt.getActualTypeArguments();
        assert
            tas.length == 3 :
            "Bound of T1 should have three type arguments";
        assert
            tas[0] instanceof TypeVariable :
            "First argument to bound of T1 is a type variable";
        assert
            tas[1] instanceof TypeVariable :
            "Second argument to bound of T1 is a type variable";
        assert
            tas[2] instanceof TypeVariable :
            "Third argument to bound of T1 is a type variable";

        TypeVariable tv1 = (TypeVariable) tas[0];
        TypeVariable tv2 = (TypeVariable) tas[1];
        TypeVariable tv3 = (TypeVariable) tas[2];

        assert
            tv1.getName().equals("T1"):
            "First type arg to bound of T1 is T1";
        assert
            tv2.getName().equals("T2"):
            "Seconmd type arg to bound of T1 is T2";
        assert
            tv3.getName().equals("T3"):
            "Third type arg to bound of T1 is T3";


    }

    static void testSuperInterfaces() {
        System.out.println("testing superinterfaces");
        Type[] sis = cls.getGenericInterfaces();
        assert
            ((sis.length == 3)):
            "C2 should have three generic superinterfaces";

        Type t = sis[0];
        assert
            t instanceof ParameterizedType :
            "First superinterface of C2 should be a parameterized type";
        ParameterizedType pt = (ParameterizedType) t;
        assert
            pt.getRawType() == I1.class :
            "First super interface of C2 is instantiation of I1";
        Type[] tas = pt.getActualTypeArguments();
        assert
            tas.length == 2 :
            "First super interface of C2 has 2 type arguments";

        t = sis[1];
        assert
            t instanceof ParameterizedType :
            "Second superinterface of C2 should be a parameterized type";
        pt = (ParameterizedType) t;
        assert
            pt.getRawType() == I2.class :
            "Second super interface of C2 is instantiation of I2";
        tas = pt.getActualTypeArguments();
        assert
            tas.length == 3 :
            "Second super interface of C2 has 3 type arguments";

        t = sis[2];
        assert
            t == I3.class :
            "Third superinterface of C2 is I3";

        // Test interfaces themselves

        TypeVariable[] tvs = I1.class.getTypeParameters();
        assert
            tvs.length == 2 :
            "I3 has two formal type parameters";
        assert
            tvs[0].getName().equals("X1") :
            "Name of first formal type arg of I1 is X1";
        assert
            tvs[1].getName().equals("X2") :
            "Name of second formal type arg of I1 is X2";

        assert
            I1.class.getGenericSuperclass() == I1.class.getSuperclass() :
            "The generic and non-generic superclasses of an interface must be the same";
        sis = I1.class.getGenericInterfaces();
        assert
            sis.length == 1 :
            "I1 has one generic superinterface";
        assert
            sis[0] == I3.class :
            "Superinterface of I1 is I3";

        tvs = I2.class.getTypeParameters();
        assert
            tvs.length == 3 :
            "I3 has three formal type parameters";
        assert
            tvs[0].getName().equals("E1") :
            "Name of first formal type arg of I2 is E1";
        assert
            tvs[1].getName().equals("E2") :
            "Name of second formal type arg of I2 is E2";
        assert
            tvs[2].getName().equals("E3") :
            "Name of third formal type arg of I2 is E3";

        assert
            I2.class.getGenericSuperclass() == I2.class.getSuperclass() :
            "The generic and non-generic superclasses of an interface must be the same";

        tvs = I3.class.getTypeParameters();
        assert
            tvs.length == 0 :
            "I3 has no formal type parameters";

        assert
            I3.class.getGenericSuperclass() == I3.class.getSuperclass() :
            "The generic and non-generic superclasses of an interface must be the same";


    }

    static void testTypeParameters() {
        System.out.println("testing type parameters");
        TypeVariable[] tvs = cls.getTypeParameters();
        assert
            tvs.length == 3 :
            "C2 should have three type parameters";
        TypeVariable tv = tvs[0];
        Type[] bs = tv.getBounds();
        assert
            bs.length == 1 :
            "T1 should have one bound";
        assert
            bs[0] instanceof ParameterizedType :
            "The bound of T1 should be a parameterized type";

        tv = tvs[1];
        bs = tv.getBounds();
        assert
            bs.length == 1 :
            "T2 should have one bound";
        assert
            bs[0] instanceof ParameterizedType :
            "The bound of T2 should be a parameterized type";

        tv = tvs[2];
        bs = tv.getBounds();
        assert
            bs.length == 1 :
            "T3 should have one bound";
        assert
            bs[0] == Throwable.class :
            "The bound of T3 should be Throwable";
    }

    static void testMethods() throws NoSuchMethodException {
        System.out.println("testing methods");



        Class[] params1 = new Class[3];
        params1[0] = Throwable.class;
        params1[1] = C0.class;
        params1[2] = C0.class;

        Class[] params2 = new Class[1];
        params2[0] = Object.class;

        Class[] params3 = new Class[1];
        params3[0] = C0.class;

        Method mc1t = cls.getMethod("mc1t", params1);
        Method mc1 = cls.getMethod("mc1", params2);
        Method mt = cls.getMethod("mt", params3);

        Type rt_mc1t = mc1t.getGenericReturnType();
        assert
            rt_mc1t  instanceof ParameterizedType :
            "The return type of mc1t should be a parameterized type";
        ParameterizedType pt = (ParameterizedType) rt_mc1t;

        assert
            pt.getRawType() == C0.class :
            "The raw return type of mc1t should be C0";

        Type[] tas = pt.getActualTypeArguments();
        assert
            tas.length == 1 :
            "Return type of mc1t should have one type argument";
        assert
            tas[0] instanceof TypeVariable :
            "Type argument of return type of mc1t is a type variable";

        Type rt_mc1 = mc1.getGenericReturnType();
        assert
            rt_mc1 == C0.class :
            "Return type of mc1 is C0";

        Type rt_mt = mt.getGenericReturnType();
        assert
            rt_mt instanceof TypeVariable :
            "Return type of mt is a type variable";

        Type[] pt_mc1t = mc1t.getGenericParameterTypes();

        assert
            pt_mc1t.length == 3 :
            "C0.mc1t has three parameters";
        Type p1_mc1t = pt_mc1t[0];
        assert p1_mc1t != null;
        assert
            p1_mc1t instanceof TypeVariable :
            "Generic type of the 1st parameter of mc1t(T) is a type variable";
        TypeVariable tv = (TypeVariable) p1_mc1t;

        assert
            tv.getName().equals("T3") :
            "Name of 1st type parameter of mc1t is T3, not " + tv.getName();
        Type[] bs = tv.getBounds();
        assert
            bs.length == 1 :
            "T3 should have one bound (mc1t)";
        assert
            bs[0] == Throwable.class :
            "The bound of T3 should be Throwable(mc1t)";

        Type p2_mc1t = pt_mc1t[1];
        assert
            p2_mc1t instanceof ParameterizedType :
            "The type of parameter 2 of mc1t is a parameterized type";
        pt = (ParameterizedType) p2_mc1t;
        assert
            pt.getRawType() == C0.class :
            "Type of parameter 2 of mc1t is instantiation of C0";
        assert
            pt.getOwnerType() == null :
            "Type of parameter 2 of mc1t is has null owner";

        tas = pt.getActualTypeArguments();
        assert
            tas.length == 1 :
            "The type of parameter 2 of mc1t has one type argument";
        Type ta = tas[0];

        assert
            ta instanceof TypeVariable :
            "The actual type arg of C0<T> is a type variable (mc1t)";
        tv = (TypeVariable) ta;
        assert
            tv.getName().equals("T") :
            "mc1t: Name of the type arg of C0<T> is T, not " + tv.getName();
        bs = tv.getBounds();
        assert
            bs.length == 1 :
            "mc1t: The type argument of C0<T>  should have one bound";
        assert
            bs[0] == Object.class :
            "mc1t: The bound of the type arg of C0<T> should be Object";

        Type p3_mc1t = pt_mc1t[2];
        assert
            p3_mc1t == C0.class :
            "Type of parameter 3 of mc1t is C0";

        Type[] pt_mc1 = mc1.getGenericParameterTypes();
        assert
            pt_mc1.length == 1 :
            "C2.mc1 has one parameter";

        Type[] pt_mt = mt.getGenericParameterTypes();
        assert
            pt_mt.length == 1 :
            "C2.mt has one parameter";
        Type p_mt = pt_mt[0];
        assert
            p_mt instanceof TypeVariable :
            "The generic type of the parameter of mt(T) is a type variable";
        tv = (TypeVariable) p_mt;
        assert
            tv.getName().equals("T2") :
            "The name of the type parameter of mt is T2, not " + tv.getName();
        bs = tv.getBounds();
        assert
            bs.length == 1 :
            "T2 should have one bound";
        assert
            bs[0] instanceof ParameterizedType:
            "The bound of T2 should be parameterized type";

        Type[] et_mc1t = mc1t.getGenericExceptionTypes();
        assert
            et_mc1t.length == 0 :
            "Method C0.mc1t should have no generic exception types";

        Type[] et_mc1 = mc1.getGenericExceptionTypes();
        assert
            et_mc1.length == 0 :
            "Method C0.mc1 should have no generic exception types";

        Type[] et_mt = mt.getGenericExceptionTypes();
        assert
            et_mt.length == 0 :
            "Method C0.mt should have no generic exception types";


        TypeVariable[] tv_mc1t = mc1t.getTypeParameters();
        assert
            tv_mc1t.length == 1 :
            "Method C2.mc1t should have one type parameter";

        TypeVariable[] tv_mc1 = mc1.getTypeParameters();
        assert
            tv_mc1.length == 2 :
            "Method C2.mc1 should have two type parameters";

        TypeVariable[] tv_mt = mt.getTypeParameters();
        assert
            tv_mt.length == 0 :
            "Method C2.mt should have no type parameters";
    }


    static void testFields() throws NoSuchFieldException{
        System.out.println("testing fields");
        Field ft = cls. getField("ft");
        Field fc1t = cls. getField("fc1t");
        Field fc1 = cls. getField("fc1");
        Field fi = cls. getField("fi");

        Type gt_ft = ft.getGenericType();
        assert
            gt_ft instanceof TypeVariable :
            "The generic type of C0.ft is a type variable";
        TypeVariable tv = (TypeVariable) gt_ft;
        assert
            tv.getName().equals("T1") :
            "The name of the type of ft is T1, not " + tv.getName();
        Type[] bs = tv.getBounds();
        assert
            bs.length == 1 :
            "The type of ft should have one bound";


        Type gt_fc1t = fc1t.getGenericType();
        assert
            gt_fc1t instanceof ParameterizedType :
            "The generic type of C0.fc1t is a parameterized type";
        ParameterizedType pt = (ParameterizedType) gt_fc1t;
        assert
            pt.getRawType() == C0.class :
            "Type of C2.fc1t is an instantiation of C0";
        assert
            pt.getOwnerType() == null :
            "Type of C2.fc1t is has null owner";
        Type[] tas = pt.getActualTypeArguments();
        assert
            tas.length == 1 :
            "The type of fc1t has one type argument";
        Type ta = tas[0];

        assert
            ta == String.class :
            "The actual type arg of C0<String> is String";


        Type gt_fc1 = fc1.getGenericType();
        assert
            gt_fc1 == C0.class :
            " Type of C2.fc1 should be C0";

        Type gt_fi = fi.getGenericType();
        assert
            gt_fi == int.class:
            " Type of C2.fi should be int";

    }

    static void testConstructors() throws NoSuchMethodException {
        System.out.println("testing constructors");
        Class[] params1 = new Class[1];
        params1[0] = C0.class;
        Constructor<C2> con = cls.getDeclaredConstructor(params1);

        Type[] pt_con = con.getGenericParameterTypes();
        assert
            pt_con.length == 1 :
            "Constructor C0(T) should have one generic parameter type";
        Type pt = pt_con[0];
        assert
            pt instanceof TypeVariable :
            "The generic type of the parameter of C0(T2) is a type variable";
        TypeVariable tv = (TypeVariable) pt;
        assert
            tv.getName().equals("T2") :
            "The name of the type parameter of C2 is T2, not " + tv.getName();
        Type[] bs = tv.getBounds();
        assert
            bs.length == 1 :
            "T should have one bound";


        Type[] et_con = con.getGenericExceptionTypes();
        assert
            et_con.length == 0 :
            "Constructor C2(T2) should have no generic exception types";

        TypeVariable[] tv_con = con.getTypeParameters();
        assert
            tv_con.length == 0 :
            "Constructor C2(T2) should have no type parameters";


        Class[] params2 = new Class[1];
        params2[0] = Object.class;
        con = cls.getDeclaredConstructor(params2);

        pt_con = con.getGenericParameterTypes();
        assert
            pt_con.length == 1 :
            "Constructor C0(T) should have one generic parameter type";
        pt = pt_con[0];
        assert
            pt instanceof TypeVariable :
            "The generic type of the parameter of C2(T) is a type variable";
        tv = (TypeVariable) pt;
        assert
            tv.getName().equals("T") :
            "The name of the type parameter of C2 is T, not " + tv.getName();
        bs = tv.getBounds();
        assert
            bs.length == 1 :
            "T should have one bound";


        et_con = con.getGenericExceptionTypes();
        assert
            et_con.length == 0 :
            "Constructor C2(T) should have no generic exception types";

        tv_con = con.getTypeParameters();
        assert
            tv_con.length == 1 :
            "Constructor C2(T) should have one type parameter";

        Class[] params3 = new Class[3];
        params3[0] = Object.class;
        params3[1] = Object.class;
        params3[2] = Object.class;

        con = cls.getDeclaredConstructor(params3);

        pt_con = con.getGenericParameterTypes();
        assert
            pt_con.length == 3 :
            "Constructor C2(T1,T2,T4) should have three generic parameter types";
        pt = pt_con[0];
        assert
            pt instanceof TypeVariable :
            "The generic type of the first parameter of C2(T1,T2,T4) is a type variable";
        tv = (TypeVariable) pt;
        assert
            tv.getName().equals("T1") :
            "The name of the type parameter of C2(T1,T2,T4) is T1, not " + tv.getName();
        bs = tv.getBounds();
        assert
            bs.length == 1 :
            "T should have one bound";


        et_con = con.getGenericExceptionTypes();
        assert
            et_con.length == 0 :
            "Constructor C2(T1,T2,T4) should have no generic exception types";

        tv_con = con.getTypeParameters();
        assert
            tv_con.length == 4 :
            "Constructor C2(T1,T2,T4) should have four type parameters";

        Class[] params4 = new Class[0];
        con = cls.getDeclaredConstructor(params4);

        pt_con = con.getGenericParameterTypes();
        assert
            pt_con.length == 0 :
            "Constructor C2() should have no generic parameter types";


        et_con = con.getGenericExceptionTypes();
        assert
            et_con.length == 1 :
            "Constructor C2() should have one generic exception type";

        tv_con = con.getTypeParameters();
        assert
            tv_con.length == 0 :
            "Constructor C2() should have no type parameters";


    }
}
