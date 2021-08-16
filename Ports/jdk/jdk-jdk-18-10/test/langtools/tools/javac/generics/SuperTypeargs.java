/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4906605
 * @summary compilation error for super.<T,E>f() and ClassName.super.<T,E>f()
 * @author gafter
 *
 * @compile  SuperTypeargs.java
 */

package superTypeargs;

import java.util.*;

class A {

    public void show() {
        System.out.println("I am being called from class A");
    }
    public String toString() {
        show();
        return "";
    }
}

class B {
    public void show() {
        System.out.println("I am being called from class B");
    }
    public String toString() {
        show();
        return "";
    }
}

class Test1<T,E> {

    public static <T,E> void check1(T val1, E val2) {
        val1.toString();
        val2.toString();
        System.out.println("Static check1 method being invoked from class Test1");
    }

    public <T,E> Test1(){
        System.out.println("The Default Test1 constructor is being called");
    }
    public <T,E> Test1(T val1, E val2) {
        System.out.println("The parameter Test1 constructor is being called");
    }
    public <T,E> int check2(T val1, E val2) {
        val1.toString();
        val2.toString();
        System.out.println("Instance method check2 being invoked from class Test1");
        return 1;
    }

}

class Test2<T,E> extends Test1<T,E> {

    public static <T,E> void check1(T val1, E val2) {
        val1.toString();
        val2.toString();
        System.out.println("Static check1 method being invoked from class Test2");
    }

    public Test2() {
        <T,E>super();
        System.out.println("The Default Test2 constructor is being called");
    }
    public <T,E> Test2(T val1, E val2) {
        <T,E>super(val1,val2);
        System.out.println("The parameter Test2 constructor is being called");
    }
    public <T,E> int check2(T val1, E val2) {
        val1.toString();
        val2.toString();
        System.out.println("Instance method check2 being invoked from class Test2");
        return 1;
    }

    public <T,E> int check3(T val1, E val2) {
        System.out.println("Instance method check3 being invoked from class Test2");
        super.<T,E>check2(val1,val2);
        /*
             ParametericMethodsTest13.java:66: <identifier> expected
             super . <T,E> check2(val1,val2);
             ^
             ParametericMethodsTest13.java:66: not a statement
             super . <T,E> check2(val1,val2);
                   ^
             2 errors
        */
        this.<T,E>check2(val1,val2);
        Test2.super.<T,E>check2(val1,val2);
        return 1;
    }

    /*
      ParametericMethodsTest14.java:130: check4(A,B) in Test2<A,B> cannot be applied to <A,B>(A,B)
      tRef.<A,B>check4(new A(), new B());
            ^
      1 error
    */
    public int check4(T val1, E val2) {
        val1.toString();
        val2.toString();
        System.out.println("Instance method check2 being invoked from class Test2");
        return 1;
    }

}

class ParametericMethodsTest14 {

    public void assertion1() {
        Test2.<A,B>check1(new A(), new B());
        Test1.<A,B>check1(new A(), new B());
        System.out.println("assertion1 passed");
    }
    public void assertion2() {
        Test2<A,B> tRef = new Test2<A,B>();
        tRef.<A,B>check1(new A(), new B());
        tRef.<A,B>check2(new A(), new B());
        Test1<A,B> tRef1 = tRef;
        tRef1.<A,B>check1(new A(), new B());
        System.out.println("assertion2 passed");
    }
    public void assertion3() {
        Test2<A,B> tRef = new Test2<A,B>();
        tRef.<A,B>check3(new A(), new B());
    }
    public void assertion4() {
        Test2<A,B> tRef = new Test2<A,B>(new A(), new B());
        tRef.<A,B>check3(new A(), new B());
    }

    public static void main(String args[]) {
        ParametericMethodsTest14 tRef = new ParametericMethodsTest14();
        tRef.assertion1();
        tRef.assertion2();
        tRef.assertion3();
        tRef.assertion4();
    }

}
