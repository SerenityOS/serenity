/*
 * @test /nodynamiccopyright/
 * @bug 8006733 8006775
 * @summary Ensure behavior for nested types is correct.
 * @author Werner Dietl
 * @compile CantAnnotateStaticClass.java
 */

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.annotation.*;

class Top {
    @Target(ElementType.TYPE_USE) @interface TA {}
    @Target(ElementType.TYPE_USE) @interface TB1 {}
    @Target(ElementType.TYPE_USE) @interface TB2 {}
    @Target(ElementType.TYPE_USE) @interface TB3 {}
    @Target(ElementType.TYPE_USE) @interface TB4 {}
    @Target(ElementType.TYPE_USE) @interface TB5 {}
    @Target(ElementType.TYPE_USE) @interface TB6 {}
    @Target(ElementType.TYPE_USE) @interface TB7 {}
    @Target(ElementType.TYPE_USE) @interface TB8 {}
    @Target(ElementType.TYPE_USE) @interface TB9 {}
    @Target(ElementType.TYPE_USE) @interface TB10 {}
    @Target(ElementType.TYPE_USE) @interface TB11 {}
    @Target(ElementType.TYPE_USE) @interface TB12 {}
    @Target(ElementType.TYPE_USE) @interface TB13 {}
    @Target(ElementType.TYPE_USE) @interface TB14 {}
    @Target(ElementType.TYPE_USE) @interface TB15 {}
    @Target(ElementType.TYPE_USE) @interface TB16 {}
    @Target(ElementType.TYPE_USE) @interface TB17 {}
    @Target(ElementType.TYPE_USE) @interface TB18 {}
    @Target(ElementType.TYPE_USE) @interface TB19 {}
    @Target(ElementType.TYPE_USE) @interface TB20 {}
    @Target(ElementType.TYPE_USE) @interface TB21 {}
    @Target(ElementType.TYPE_USE) @interface TB22 {}
    @Target(ElementType.TYPE_USE) @interface TB23 {}
    @Target(ElementType.TYPE_USE) @interface TB24 {}
    @Target(ElementType.TYPE_USE) @interface TB25 {}
    @Target(ElementType.TYPE_USE) @interface TB26 {}
    @Target(ElementType.TYPE_USE) @interface TB27 {}
    @Target(ElementType.TYPE_USE) @interface TB28 {}
    @Target(ElementType.TYPE_USE) @interface TB29 {}
    @Target(ElementType.TYPE_USE) @interface TB30 {}
    @Target(ElementType.TYPE_USE) @interface TB31 {}
    @Target(ElementType.TYPE_USE) @interface TB32 {}
    @Target(ElementType.TYPE_USE) @interface TB33 {}
    @Target(ElementType.TYPE_USE) @interface TB34 {}
    @Target(ElementType.TYPE_USE) @interface TB35 {}
    @Target(ElementType.TYPE_USE) @interface TB36 {}
    @Target(ElementType.TYPE_USE) @interface TB37 {}
    @Target(ElementType.TYPE_USE) @interface TB38 {}
    @Target(ElementType.TYPE_USE) @interface TB39 {}
    @Target(ElementType.TYPE_USE) @interface TB40 {}
    @Target(ElementType.TYPE_USE) @interface TB41 {}
    @Target(ElementType.TYPE_USE) @interface TC {}

    class Outer {
        class Inner {
            Object o1 = Top.this;
            Object o2 = Outer.this;
            Object o3 = this;
        }
        // Illegal
        // static class SInner {}
        // interface IInner {}
    }

    // All combinations are OK

    Top.@TB1 Outer f1;
    @TB2 Outer.Inner f1a;
    Outer. @TC Inner f1b;
    @TB3 Outer. @TC Inner f1c;

    @TA Top. @TB4 Outer f2;
    @TA Top. @TB5 Outer.Inner f2a;
    @TA Top. Outer. @TC Inner f2b;
    @TA Top. @TB6 Outer. @TC Inner f2c;

    @TB7 Outer f1r() { return null; }
    @TB8 Outer.Inner f1ra() { return null; }
    Outer. @TC Inner f1rb() { return null; }
    @TB9 Outer. @TC Inner f1rc() { return null; }

    void f1param(@TB41 Outer p,
            @TB10 Outer.Inner p1,
            Outer. @TC Inner p2,
            @TB11 Outer. @TC Inner p3) { }

    void f1cast(Object o) {
        Object l;
        l = (@TB12 Outer) o;
        l = (@TB13 Outer.Inner) o;
        l = (Outer. @TC Inner) o;
        l = (@TB14 Outer. @TC Inner) o;
    }

    List<@TB15 Outer> g1;
    List<@TB16 Outer.Inner> g1a;
    List<Outer. @TC Inner> g1b;
    List<@TB17 Outer. @TC Inner> g1c;

    List<@TA Top. @TB18 Outer> g2;
    List<@TA Top. @TB19 Outer.Inner> g2a;
    List<@TA Top. Outer. @TC Inner> g2b;
    List<@TA Top. @TB20 Outer. @TC Inner> g2c;

    List<@TB21 Outer> g1r() { return null; }
    List<@TB22 Outer.Inner> g1ra() { return null; }
    List<Outer. @TC Inner> g1rb() { return null; }
    List<@TB23 Outer. @TC Inner> g1rc() { return null; }

    void g1param(List<@TB24 Outer> p,
            List<@TB25 Outer.Inner> p1,
            List<Outer. @TC Inner> p2,
            List<@TB26 Outer. @TC Inner> p3) { }

    void g1new(Object o) {
        Object l;
        l = new @TB27 ArrayList<@TB28 Outer>();
        l = new @TB29 ArrayList<@TB30 Outer.Inner>();
        l = new @TB31 HashMap<String, Outer. @TC Inner>();
        l = new @TB32 HashMap<String, @TB33 Outer. Inner>();
        l = new @TB34 HashMap<String, @TB35 Outer. @TC Inner>();
        l = new @TB36 HashMap<String, @TA Top. Outer. @TC Inner>();
        l = new @TB37 HashMap<String, @TA Top. @TB38 Outer. Inner>();
        l = new @TB39 HashMap<String, @TA Top. @TB40 Outer. @TC Inner>();
    }
}
