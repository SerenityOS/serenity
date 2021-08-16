/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.*;
import java.util.Map;

/*
 * @test
 * @bug 8006775
 * @summary new type annotation location: nested types
 * @author Werner Dietl
 * @compile NestedTypes.java
 */
class Outer {
    class Inner {
        class Inner2 {
            // m1a-c all have the same parameter type.
            void m1a(@A Inner2 p1a) {}
            void m1b(Inner.@A Inner2 p1b) {}
            void m1c(Outer.Inner.@A Inner2 p1c) {}
            // notice the difference to m1d
            void m1d(@A Outer.Inner.Inner2 p1d) {}

            // m2a-b both have the same parameter type.
            void m2a(@A Inner.Inner2 p2a) {}
            void m2b(Outer.@A Inner.Inner2 p2b) {}

            // The location for @A is the same in m3a-c
            void m3a(@A Outer p3a) {}
            void m3b(@A Outer.Inner p3b) {}
            void m3c(@A Outer.Inner.Inner2 p3c) {}

            // Test combinations
            void m4a(@A Outer p3a) {}
            void m4b(@A Outer. @B Inner p3b) {}
            void m4c(@A Outer. @B Inner. @C Inner2 p3c) {}
        }
    }

    void m4a(@A Map p4a) {}
    void m4b(Map.@B Entry p4c) {}
    // Illegal:
    // void m4b(@A Map.Entry p4b) {}
    // void m4c(@A Map.@B Entry p4c) {}

    void m4c(Map<String,String>.@B Entry<String,String> p4d) {}
    // Illegal:
    // void m4d(@A Map<String,String>.@B Entry<String,String> p4d) {}

    void m4e(MyList<Map.Entry> p4e) {}
    void m4f(MyList<Map.@B Entry> p4f) {}
    // Illegal:
    // void m4g(MyList<@A Map.Entry> p4e) {}
    // void m4h(MyList<@A Map.@B Entry> p4f) {}

    class GInner<X> {
        class GInner2<Y, Z> {}
    }

    static class Static {}
    static class GStatic<X, Y> {
        static class GStatic2<Z> {}
    }
}

class Test1 {
    // Outer.GStatic<Object,Object>.GStatic2<Object> gs;
    Outer.GStatic.@A GStatic2<Object> gsgood;
    // TODO: add failing test
    // Outer.@A GStatic.GStatic2<Object> gsbad;

    MyList<@A Outer . @B Inner. @C Inner2> f;
    @A Outer .GInner<Object>.GInner2<String, Integer> g;

    // TODO: Make sure that something like this fails gracefully:
    // MyList<java.@B lang.Object> pkg;

    @A Outer f1;
    @A Outer . @B Inner f2 = f1.new @B Inner();
    // TODO: ensure type annos on new are stored.
    @A Outer . @B GInner<@C Object> f3 = f1.new @B GInner<@C Object>();

    MyList<@A Outer . @B GInner<@C MyList<@D Object>>. @E GInner2<@F Integer, @G Object>> f4;
    // MyList<Outer.GInner<Object>.GInner2<Integer>> f4clean;

    @A Outer . @B GInner<@C MyList<@D Object>>. @E GInner2<@F Integer, @G Object> f4top;

    MyList<@A Outer . @B GInner<@C MyList<@D Object @E[] @F[]>>. @G GInner2<@H Integer, @I Object> @J[] @K[]> f4arr;

    @A Outer . @B GInner<@C MyList<@D Object @E[] @F[]>>. @G GInner2<@H Integer, @I Object> @J[] @K[] f4arrtop;

    MyList<Outer . @B Static> f5;
    // Illegal:
    // MyList<@A Outer . @B Static> f5;

    Outer . @B Static f6;
    // Illegal:
    // @A Outer . @B Static f6;

    Outer . @Bv("B") GStatic<@Cv("C") String, @Dv("D") Object> f7;
    // Illegal:
    // @Av("A") Outer . @Bv("B") GStatic<@Cv("C") String, @Dv("D") Object> f7;

    Outer . @Cv("Data") Static f8;
    // Illegal:
    // @A Outer . @Cv("Data") Static f8;

    MyList<Outer . @Cv("Data") Static> f9;
    // Illegal:
    // MyList<@A Outer . @Cv("Data") Static> f9;

}

class Test2 {
    void m() {
        @A Outer f1 = null;
        @A Outer.@B Inner f2 = null;
        Outer.@B Static f3 = null;
        // Illegal:
        // @A Outer.@B Static f3 = null;
        @A Outer.@C Inner f4 = null;

        Outer . @B Static f5 = null;
        Outer . @Cv("Data") Static f6 = null;
        MyList<Outer . @Cv("Data") Static> f7 = null;
    }
}

class Test3 {
    void monster(@A Outer p1,
        @A Outer.@B Inner p2,
        Outer.@B Static p3,
        @A Outer.@Cv("Test") Inner p4,
        Outer . @B Static p5,
        Outer . @Cv("Data") Static p6,
        MyList<Outer . @Cv("Data") Static> p7) {
    }
}

class Test4 {
    void m() {
        @A Outer p1 = new @A Outer();
        @A Outer.@B Inner p2 = p1.new @B Inner();
        // Illegal:
        // @A Outer.@B Static p3 = new @A Outer.@B Static();
        // Object o3 = new @A Outer.@B Static();

        @A Outer.@Cv("Test") Inner p4 = p1.new @Cv("Test") Inner();
        Outer . @B Static p5 = new Outer . @B Static();
        Outer . @Cv("Data") Static p6 = new Outer . @Cv("Data") Static();
        MyList<Outer . @Cv("Data") Static> p7 = new MyList<Outer . @Cv("Data") Static>();
    }
}

class MyList<K> { }


@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface A { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface B { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface C { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface D { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface E { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface F { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface G { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface H { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface I { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface J { }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface K { }

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface Av { String value(); }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface Bv { String value(); }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface Cv { String value(); }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface Dv { String value(); }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface Ev { String value(); }
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface Fv { String value(); }

