/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import p2.D;

interface InterfaceA {
    public void Method1();
    public void Method2();
}

class ClassA implements InterfaceA {
    public void Method1() {
        System.out.println("ClassA's Method1");
    }
    public void Method2() {
        System.out.println("ClassA's Method2");
    }
    public void Method3() {
        System.out.println("ClassA's Method3");
    }
    public void Method4() {
        System.out.println("ClassA's Method4");
    }
}

public class ClassB extends ClassA {
    public void Method1() {
        System.out.println("ClassB's Method1");
    }
    public void Method3() {
        System.out.println("ClassB's Method3");
    }
    public static void main(String[] args) throws Exception {
        ClassB classBObj = new ClassB();
        classBObj.Method1();
        classBObj.Method2();
        classBObj.Method3();
        classBObj.Method4();
        ClassA classAObj = new ClassA();
        classAObj.Method1();
        classAObj.Method2();
        classAObj.Method3();
        classAObj.Method4();
        D classD = new D();
        D.loadD();
    }
}
