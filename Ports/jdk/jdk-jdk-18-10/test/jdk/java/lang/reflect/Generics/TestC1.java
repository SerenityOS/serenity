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
* @compile TestC1.java
* @run main/othervm -ea TestC1
*/


import java.lang.reflect.*;


abstract class C1<T> {

    public T ft;
    public C1<T> fc1t;
    public C1 fc1;

    public C1(T t) {}

    public abstract  C1<T> mc1t(T t, C1<T> c1t, C1 c1);

    public abstract C1 mc1();

    public abstract T mt(T t);

}

public class TestC1 {

    static Class<C1> cls = C1.class;
    static {
        TestC1.class.getClassLoader().setDefaultAssertionStatus(true);
    }



    public static void main(String[] args) throws Throwable {
        testSuperclass();
        testSuperInterfaces();
        testTypeParameters();
        testMethods();
        testConstructor();
        testFields();
    }

    static void testSuperclass() {
        System.out.println("testing superclass");
        Type sc = cls.getGenericSuperclass();
        assert
            (sc == Object.class) :
            "The generic superclass of C1 should be Object";
    }

    static void testSuperInterfaces() {
        System.out.println("testing superinterfaces");
        Type[] sis = cls.getGenericInterfaces();
        assert
            (sis.length == 0) :
            "C1 should have no generic superinterfaces";
    }

    static void testTypeParameters() {
        System.out.println("testing type parameters");
        TypeVariable[] tvs = cls.getTypeParameters();
        assert
            tvs.length == 1 :
            "C1 should have one type parameter";
        TypeVariable tv = tvs[0];
        Type[] bs = tv.getBounds();
        assert
            bs.length == 1 :
            "T should have one bound";
        assert
            bs[0] == Object.class :
            "The default bound of a type variable should be Object";
    }

    static void testMethods() throws NoSuchMethodException {
        System.out.println("testing methods");
        Class[] params1 = new Class[3];
        params1[0] = Object.class;
        params1[1] = cls;
        params1[2] = cls;

        Class[] params3 = new Class[1];
        params3[0] = Object.class;

        Method mc1t = cls.getMethod("mc1t", params1);
        Method mc1 = cls.getMethod("mc1", new Class[0]);
        Method mt = cls.getMethod("mt", params3);

        Type rt_mc1t = mc1t.getGenericReturnType();
        Type rt_mc1 = mc1.getGenericReturnType();
        Type rt_mt = mt.getGenericReturnType();

        Type[] pt_mc1t = mc1t.getGenericParameterTypes();

        assert
            pt_mc1t.length == 3 :
            "C1.mc1t has three parameters";
        Type p1_mc1t = pt_mc1t[0];
        assert p1_mc1t != null;
        assert
            p1_mc1t instanceof TypeVariable :
            "Generic type of the 1st parameter of mc1t(T) is a type variable";
        TypeVariable tv = (TypeVariable) p1_mc1t;

        assert
            tv.getName().equals("T") :
            "Name of 1st type parameter of mc1t is T, not " + tv.getName();
        Type[] bs = tv.getBounds();
        assert
            bs.length == 1 :
            "T should have one bound (mc1t)";
        assert
            bs[0] == Object.class :
            "The bound of T should be Object (mc1t)";

        Type p2_mc1t = pt_mc1t[1];

        assert
            p2_mc1t instanceof ParameterizedType :
            "The type of parameter 2 of mc1t is a parameterized type";
        ParameterizedType pt = (ParameterizedType) p2_mc1t;
        assert
            pt.getRawType() == cls :
            "Type of parameter 2 of mc1t is instantiation of C1";
        assert
            pt.getOwnerType() == null :
            "Type of parameter 2 of mc1t is has null owner";

        Type[] tas = pt.getActualTypeArguments();
        assert
            tas.length == 1 :
            "The type of parameter 2 of mc1t has one type argument";
        Type ta = tas[0];

        assert
            ta instanceof TypeVariable :
            "The actual type arg of C1<T> is a type variable (mc1t)";
        tv = (TypeVariable) ta;
        assert
            tv.getName().equals("T") :
            "mc1t: Name of the type arg of C1<T> is T, not " + tv.getName();
        bs = tv.getBounds();
        assert
            bs.length == 1 :
            "mc1t: The type argument of C1<T>  should have one bound";
        assert
            bs[0] == Object.class :
            "mc1t: The bound of the type arg of C1<T> should be Object";

        Type p3_mc1t = pt_mc1t[2];

        assert
            p3_mc1t == cls :
            "Type of parameter 3 of mc1t is C1";

        Type[] pt_mc1 = mc1.getGenericParameterTypes();
        assert
            pt_mc1.length == 0 :
            "C1.mc1 has zero parameters";

        Type[] pt_mt = mt.getGenericParameterTypes();
        assert
            pt_mt.length == 1 :
            "C1.mt has one parameter";
        Type p_mt = pt_mt[0];
        assert
            p_mt instanceof TypeVariable :
            "The generic type of the parameter of mt(T) is a type variable";
        tv = (TypeVariable) p_mt;
        assert
            tv.getName().equals("T") :
            "The name of the type parameter of mt is T, not " + tv.getName();
        bs = tv.getBounds();
        assert
            bs.length == 1 :
            "T should have one bound";
        assert
            bs[0] == Object.class :
            "The bound of T should be Object";

        Type[] et_mc1t = mc1t.getGenericExceptionTypes();
        assert
            et_mc1t.length == 0 :
            "Method C1.mc1t should have no generic exception types";

        Type[] et_mc1 = mc1.getGenericExceptionTypes();
        assert
            et_mc1.length == 0 :
            "Method C1.mc1 should have no generic exception types";

        Type[] et_mt = mt.getGenericExceptionTypes();

        assert
            et_mt.length == 0 :
            "Method C1.mt should have no generic exception types";


        TypeVariable[] tv_mc1t = mc1t.getTypeParameters();
        assert
            tv_mc1t.length == 0 :
            "Method C1.mc1t should have no type parameters";

        TypeVariable[] tv_mc1 = mc1.getTypeParameters();
        assert
            tv_mc1.length == 0 :
            "Method C1.mc1 should have no type parameters";

        TypeVariable[] tv_mt = mt.getTypeParameters();
        assert
            tv_mt.length == 0 :
            "Method C1.mt should have no type parameters";
    }


    static void testFields() throws NoSuchFieldException{
        System.out.println("testing fields");
        Field ft = cls. getField("ft");
        Field fc1t = cls. getField("fc1t");
        Field fc1 = cls. getField("fc1");

        Type gt_ft = ft.getGenericType();
        assert
            gt_ft instanceof TypeVariable :
            "The generic type of C1.ft is a type variable";
        TypeVariable tv = (TypeVariable) gt_ft;
        assert
            tv.getName().equals("T") :
            "The name of the type of ft is T, not " + tv.getName();
        Type[] bs = tv.getBounds();
        assert
            bs.length == 1 :
            "The type of ft should have one bound";
        assert
            bs[0] == Object.class :
            "The bound of the type of ft should be Object";

        Type gt_fc1t = fc1t.getGenericType();
        assert
            gt_fc1t instanceof ParameterizedType :
            "The generic type of C1.fc1t is a parameterized type";
        ParameterizedType pt = (ParameterizedType) gt_fc1t;
        assert
            pt.getRawType() == cls :
            "Type of C1.fc1t is instantiation of C1";
        assert
            pt.getOwnerType() == null :
            "Type of C1.fc1t is has null owner";
        Type[] tas = pt.getActualTypeArguments();
        assert
            tas.length == 1 :
            "The type of fc1t has one type argument";
        Type ta = tas[0];

        assert
            ta instanceof TypeVariable :
            "The actual type arg of C1<T> is a type variable";
        tv = (TypeVariable) ta;
        assert
            tv.getName().equals("T") :
            "The name of the type arg of C1<T> is T, not " + tv.getName();
        bs = tv.getBounds();
        assert
            bs.length == 1 :
            "The type argument of C1<T>  should have one bound";
        assert
            bs[0] == Object.class :
            "The bound of the type arg of C1<T> should be Object";

        Type gt_fc1 = fc1.getGenericType();
        assert
            gt_fc1 == cls :
            " Type of C1.fc1 should be C1";
    }

    static void testConstructor() throws NoSuchMethodException {
        System.out.println("testing constructors");
        Class[] params = new Class[1];
        params[0] = Object.class;
        Constructor<C1> con = cls.getDeclaredConstructor(params);

        Type[] pt_con = con.getGenericParameterTypes();
        assert
            pt_con.length == 1 :
            "Constructor C1(T) should have one generic parameter type";
        Type pt = pt_con[0];
        assert
            pt instanceof TypeVariable :
            "The generic type of the parameter of C1(T) is a type variable";
        TypeVariable tv = (TypeVariable) pt;
        assert
            tv.getName().equals("T") :
            "The name of the type parameter of C is T, not " + tv.getName();
        Type[] bs = tv.getBounds();
        assert
            bs.length == 1 :
            "T should have one bound";
        assert
            bs[0] == Object.class :
            "The bound of T should be Object";

        Type[] et_con = con.getGenericExceptionTypes();
        assert
            et_con.length == 0 :
            "Constructor C1(T) should have no generic exception types";

        TypeVariable[] tv_con = con.getTypeParameters();
        assert
            tv_con.length == 0 :
            "Constructor C1(T) should have no type parameters";

    }
}
