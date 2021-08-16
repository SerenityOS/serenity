/*
 * @test /nodynamiccopyright/
 * @bug 8006733 8006775
 * @summary Ensure behavior for nested types is correct.
 * @author Werner Dietl
 * @ignore 8057679 clarify error messages trying to annotate scoping
 * @ignore 8057683 improve ordering of errors with type annotations
 * @compile/fail/ref=CantAnnotateStaticClass2.out -XDrawDiagnostics CantAnnotateStaticClass2.java
 */

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.annotation.*;

class Top {
    @Target(ElementType.TYPE_USE)
    @interface TA {}

    @Target(ElementType.TYPE_USE)
    @interface TB {}

    @Target(ElementType.TYPE_USE)
    @interface TC {}

    static class Outer {
        class Inner {
            // Object o1 = Top.this;
            Object o2 = Outer.this;
            Object o3 = this;
        }
        static class SInner {
            // Object o1 = Top.this;
            // Object o2 = Outer.this;
            Object o3 = this;
        }
        interface IInner {
            // Object o1 = Top.this;
            // Object o2 = Outer.this;
            // Object o3 = this;
        }
    }

    @TB Outer f1;
    @TB Outer.Inner f1a;
    @TB Outer.SInner f2a; // err
    @TB Outer.IInner f3a; // err

    Outer. @TC Inner f1b;
    Outer. @TC SInner f2b;
    Outer. @TC IInner f3b;

    @TB Outer. @TC Inner f1c;
    @TB Outer. @TC SInner f2c; // err
    @TB Outer. @TC IInner f3c; // err

    @TA Top. @TB Outer g1; // err
    @TA Top. @TB Outer.Inner g1a; // err
    @TA Top. @TB Outer.SInner g2a; // err
    @TA Top. @TB Outer.IInner g3a; // err

    @TA Top. Outer. @TC Inner g1b; // err
    @TA Top. Outer. @TC SInner g2b; // err
    @TA Top. Outer. @TC IInner g3b; // err

    @TA Top. @TB Outer. @TC Inner g1c; // err
    @TA Top. @TB Outer. @TC SInner g2c; // err
    @TA Top. @TB Outer. @TC IInner g3c; // err

    @TB Outer f1r() { return null; }

    @TB Outer.Inner f1ra() { return null; }
    @TB Outer.SInner f2ra() { return null; } // err
    @TB Outer.IInner f3ra() { return null; } // err

    Outer. @TC Inner f1rb() { return null; }
    Outer. @TC SInner f2rb() { return null; }
    Outer. @TC IInner f3rb() { return null; }

    @TB Outer. @TC Inner f1rc() { return null; }
    @TB Outer. @TC SInner f2rc() { return null; } // err
    @TB Outer. @TC IInner f3rc() { return null; } // err

    void f1param(@TB Outer p,
            @TB Outer.Inner p1,
            Outer. @TC Inner p2,
            @TB Outer. @TC Inner p3) { }
    void f2param(@TB Outer p,
            @TB Outer.SInner p1, // err
            Outer. @TC SInner p2,
            @TB Outer. @TC SInner p3) { } // err
    void f3param(@TB Outer p,
            @TB Outer.IInner p1, // err
            Outer. @TC IInner p2,
            @TB Outer. @TC IInner p3) { } // err

    void f1cast(Object o) {
        Object l;
        l = (@TB Outer) o;
        l = (@TB Outer.Inner) o;
        l = (Outer. @TC Inner) o;
        l = (@TB Outer. @TC Inner) o;
    }
    void f2cast(Object o) {
        Object l;
        l = (@TB Outer) o;
        l = (@TB Outer.SInner) o; // err
        l = (Outer. @TC SInner) o;
        l = (@TB Outer. @TC SInner) o; // err
    }
    void f3cast(Object o) {
        Object l;
        l = (@TB Outer) o;
        l = (@TB Outer.IInner) o; // err
        l = (Outer. @TC IInner) o;
        l = (@TB Outer. @TC IInner) o; // err
    }

    List<@TB Outer> h1;

    List<@TB Outer.Inner> h1a;
    List<@TB Outer.SInner> h2a; // err
    List<@TB Outer.IInner> h3a; // err

    List<Outer. @TC Inner> h1b;
    List<Outer. @TC SInner> h2b;
    List<Outer. @TC IInner> h3b;

    List<@TB Outer. @TC Inner> h1c;
    List<@TB Outer. @TC SInner> h2c; // err
    List<@TB Outer. @TC IInner> h3c; // err

    List<@TA Top. @TB Outer> k1; // err

    List<@TA Top. @TB Outer.Inner> k1a; // err
    List<@TA Top. @TB Outer.SInner> k2a; // err
    List<@TA Top. @TB Outer.IInner> k3a; // err

    List<@TA Top. Outer. @TC Inner> k1b; // err
    List<@TA Top. Outer. @TC SInner> k2b; // err
    List<@TA Top. Outer. @TC IInner> k3b; // err

    List<@TA Top. @TB Outer. @TC Inner> k1c; // err
    List<@TA Top. @TB Outer. @TC SInner> k2c; // err
    List<@TA Top. @TB Outer. @TC IInner> k3c; // err


    List<@TB Outer> g1r() { return null; }

    List<@TB Outer.Inner> g1ra() { return null; }
    List<@TB Outer.SInner> g2ra() { return null; } // err
    List<@TB Outer.IInner> g3ra() { return null; } // err

    List<Outer. @TC Inner> g1rb() { return null; }
    List<Outer. @TC SInner> g2rb() { return null; }
    List<Outer. @TC IInner> g3rb() { return null; }

    List<@TB Outer. @TC Inner> g1rc() { return null; }
    List<@TB Outer. @TC SInner> g2rc() { return null; } // err
    List<@TB Outer. @TC IInner> g3rc() { return null; } // err

    void g1param(List<@TB Outer> p,
            List<@TB Outer.Inner> p1,
            List<Outer. @TC Inner> p2,
            List<@TB Outer. @TC Inner> p3) { }
    void g2param(List<@TB Outer> p,
            List<@TB Outer.SInner> p1, // err
            List<Outer. @TC SInner> p2,
            List<@TB Outer. @TC SInner> p3) { } // err
    void g3param(List<@TB Outer> p,
            List<@TB Outer.IInner> p1, // err
            List<Outer. @TC IInner> p2,
            List<@TB Outer. @TC IInner> p3) { } // err

    void g1new(Object o) {
        Object l;
        l = new @TB ArrayList<@TB Outer>();
        l = new @TB ArrayList<@TB Outer.Inner>();
        l = new @TB HashMap<String, Outer. @TC Inner>();
        l = new @TB HashMap<String, @TB Outer. Inner>();
        l = new @TB HashMap<String, @TB Outer. @TC Inner>();
    }
    void g2new(Object o) {
        Object l;
        l = new @TB ArrayList<@TB Outer>();
        l = new @TB ArrayList<@TB Outer.SInner>(); // err
        l = new @TB HashMap<String, Outer. @TC SInner>();
        l = new @TB HashMap<String, @TB Outer. SInner>(); // err
        l = new @TB HashMap<String, @TB Outer. @TC SInner>(); // err
    }
    void g3new(Object o) {
        Object l;
        l = new @TB ArrayList<@TB Outer>();
        l = new @TB ArrayList<@TB Outer.IInner>(); // err
        l = new @TB HashMap<String, Outer. @TC IInner>();
        l = new @TB HashMap<String, @TB Outer. IInner>(); // err
        l = new @TB HashMap<String, @TB Outer. @TC IInner>(); // err
    }
    void g4new(Object o) {
        Object l;
        l = new @TB ArrayList<@TA Top. @TB Outer>(); // err
        l = new @TB ArrayList<@TA Top. @TB Outer.IInner>(); // err
        l = new @TB HashMap<String, @TA Top. Outer. @TC IInner>(); // err
        l = new @TB HashMap<String, @TA Top. @TB Outer. IInner>(); // err
        l = new @TB HashMap<String, @TA Top. @TB Outer. @TC IInner>(); // err
        l = new @TB HashMap<String, @TA @TB @TC Top. Outer. IInner>(); // err
    }
}
