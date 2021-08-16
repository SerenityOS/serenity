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
* @compile TestN1.java
* @run main/othervm -ea TestN1
*/


import java.lang.reflect.*;


class N1<T1, T2> {

    public Inner1 i1;
    public Inner2 i2;
    public Inner2<? super Character> i2sc;

    public class Inner1 {

    }

    public class Inner2<T1> {
        public boolean x;
        public byte b;
        public short s;
        public char c;
        public int i;
        public long l;
        public float f;
        public double d;

        public boolean[] xa;
        public byte[] ba;
        public short[] sa;
        public char[] ca;
        public int[] ia;
        public long[] la;
        public float[] fa;
        public double[] da;
    }

    public class Inner3<X1, X2, X3> {
        X1 x1;

        Inner3(X1 x1, X2 x2, X3 x3, T1 t1, T2 t2) {}

        <T, R, S> Inner3(T t, R r, S s, X1 x1) {}

        int shazam(boolean b, short s, int[] ia, Object[] oa, Inner1 i1,
                   Inner1 i1a, InnerInner<String,
                   Inner3<Object, String, Object[]>> ii)
        { return 3;}

        public class InnerInner<T2, X2> {

            boolean b;
            Inner2<X2> i2x;

            void foo(X3 x3){}
            <X3> X3[] bar(X1 x1, X3[] x3, T1 t1) { return x3;}
            N1<X1, X2> baz(N1<X1, X2> n1) { return n1;}
            N1<?, ?> bam(N1<T1, X2> n1) { return n1;}
            N1<? extends T1, ?> boom(N1<T1, X2> n1) { return n1;}

        }

    }




}


public class TestN1 {

    static Class<N1> cls = N1.class;


    public static void main(String[] args) throws Throwable {
        testTypeParameters();
        testInner1();
        testInner2();
        testInner3();
    }


    static void testTypeParameters() {

        System.out.println("testing type parameters");
        TypeVariable[] tvs = cls.getTypeParameters();
        assert
            tvs.length == 2 :
            "N1 should have two type parameters";
    }


    static void testInner1() {
        System.out.println("testing non-generic inner class");
        Class in1 = N1.Inner1.class;

        TypeVariable[] tvs = in1.getTypeParameters();
        assert
            tvs.length == 0 :
            "N1.Inner2 should have no type parameters";

    }

    static void testInner2() throws NoSuchFieldException {
        System.out.println("testing generic inner class 1");
        Class in1 = N1.Inner2.class;

        TypeVariable[] tvs = in1.getTypeParameters();
        assert
            tvs.length == 1 :
            "N1.Inner2 should have one type parameter";


        assert
            in1.getField("x").getGenericType() == boolean.class :
            "Type of Inner2.x should be boolean";

        assert
            in1.getField("b").getGenericType() == byte.class :
            "Type of Inner2.b should be byte";
        assert
            in1.getField("s").getGenericType() == short.class :
            "Type of Inner2.s should be short";
        assert
            in1.getField("c").getGenericType() == char.class :
            "Type of Inner2.x should be char";
        assert
            in1.getField("i").getGenericType() == int.class :
            "Type of Inner2.i should be int";
        assert
            in1.getField("l").getGenericType() == long.class :
            "Type of Inner2.l should be long";
        assert
            in1.getField("f").getGenericType() == float.class :
            "Type of Inner2.f should be float";
        assert
            in1.getField("d").getGenericType() == double.class :
            "Type of Inner2.d should be double";

        assert
            in1.getField("xa").getGenericType() == boolean[].class :
            "Type of Inner2.xa should be boolean[]";

        assert
            in1.getField("ba").getGenericType() == byte[].class :
            "Type of Inner2.ba should be byte[]";
        assert
            in1.getField("sa").getGenericType() == short[].class :
            "Type of Inner2.sa should be short[]";
        assert
            in1.getField("ca").getGenericType() == char[].class :
            "Type of Inner2.xa should be char[]";
        assert
            in1.getField("ia").getGenericType() == int[].class :
            "Type of Inner2.ia should be int[]";
        assert
            in1.getField("la").getGenericType() == long[].class :
            "Type of Inner2.la should be long[]";
        assert
            in1.getField("fa").getGenericType() == float[].class :
            "Type of Inner2.fa should be float[]";
        assert
            in1.getField("da").getGenericType() == double[].class :
            "Type of Inner2.da should be double[]";
    }


    static void testInner3() {
        System.out.println("testing generic inner class 3");
        Class in1 = N1.Inner3.class;

        TypeVariable[] tvs = in1.getTypeParameters();
        assert
            tvs.length == 3 :
            "N1.Inner2 should have three type parameters";
    }
}
