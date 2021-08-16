/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdb.print.print002;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class print002a {

    static print002a _print002a = new print002a();

    public static void main(String args[]) {
       System.exit(print002.JCK_STATUS_BASE + _print002a.runIt(args, System.out));
    }

    static void lastBreak () {}

    public int runIt(String args[], PrintStream out) {
        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        int i = 2;
        int j = 6;
        boolean b1 = true;
        boolean b2 = false;

        A a = new A();

        synchronized (this) {
            lastBreak();
        }

        log.display("Debuggee PASSED");
        return print002.PASSED;
    }
}

class A {
B b;
A() { b = new B(); }
}

class B {
C c;
B() { c = new C(); }
}

class C {
D d;
C() { d = new D(); }
}

class D {
E e;
D() { e = new E(); }
}

class E {
F f;
E() { f = new F(); }
}

class F {
G g;
F() { g = new G(); }
}

class G {
H h;
G() { h = new H(); }
}

class H {
I i;
H() { i = new I(); }
}

class I {
J j;
I() { j = new J(); }
}

class J {
K k;
J() { k = new K(); }
}

class K {
L l;
K() { l = new L(); }
}

class L {
M m;
L() { m = new M(); }
}

class M {
N n;
M() { n = new N(); }
}

class N {
O o;
N() { o = new O(); }
}

class O {
P p;
O() { p = new P(); }
}

class P {
Q q;
P() { q = new Q(); }
}

class Q {
R r;
Q() { r = new R(); }
}

class R {
S s;
R() { s = new S(); }
}

class S {
T t;
S() { t = new T(); }
}

class T {
U u;
T() { u = new U(); }
}

class U {
V v;
U() { v = new V(); }
}

class V {
W w;
V() { w = new W(); }
}

class W {
X x;
W() { x = new X(); }
}

class X {
Y y;
X() { y = new Y(); }
}

class Y {
Z z;
Y() { z = new Z(); }
}

class Z {
  String s;
  Z() { s  = "foo";}
}
