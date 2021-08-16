/**
 * @test  /nodynamiccopyright/
 * @bug 4216683 4346296 4656556 4785453 8164073
 * @summary New rules for when deprecation messages are suppressed
 * @author gafter
 *
 * @compile/ref=SuppressDeprecation.out -Xlint:deprecation -XDrawDiagnostics SuppressDeprecation.java
 * @compile/ref=SuppressDeprecation8.out -source 8 -Xlint:deprecation -XDrawDiagnostics -Xlint:-options SuppressDeprecation.java
 */

/* Test for the contexts in which deprecations warnings should
 * (and should not) be given.  They should be given when
 * o  invoking a deprecated method from a non-deprecated one.
 * o  new X() using a deprecated constructor
 * o  super() to a deprecated constructor
 * o  extending a deprecated class.
 * But deprecation messages are suppressed as follows:
 * o  Never complain about code in the same outermost class as
 *    the deprecated entity.
 * o  Extending a deprecated class with a deprecated one is OK.
 * o  Overriding a deprecated method with a deprecated one is OK.
 * o  Code appearing in a deprecated class is OK.
 *
 */

class T {
    /** var.
     *  @deprecated . */
    int var;

    /** f.
     *  @deprecated . */
    void f() {
    }

    /** g.
     *  @deprecated . */
    void g() {
        f();
    }

    void h() {
        f();
    }

    /** T.
     *  @deprecated . */
    T() {
    }

    /** T.
     *  @deprecated . */
    T(int i) {
        this();
    }

    T(float f) {
        this();
    }

    void xyzzy() {
        new T();
        new T(1.4f);
    }
    /** plugh.
     *  @deprecated . */
    void plugh() {
        new T();
        new T(1.45f);
    }

    /** calcx..
     *  @deprecated . */
    int calcx() { return 0; }
}

class U extends T {
    /** f.
     * @deprecated . */
    void f() {
    }

    void g() { // error (1)
        super.g(); // error (2)
        var = 12; // error (3)
    }

    U() {} // error (4)

    U(int i) {
        super(i); // error (5)
    }

    U(float f) {
        super(1.3f);
    }
}

class V extends T {} // error (6)

/** W.
 * @deprecated . */
class W extends T { // ok - inside deprecated class
    /** W.
     * @deprecated . */
    static {
        new T(1.3f).g(); // ok - called from deprecated static block
    }

    /** W.
     * @deprecated . */
    {
        new T(1.3f).g(); // ok - called from deprecated block
    }

    {
        new T(1.3f).g(); // ok - inside deprecated class
    }

    int x = calcx(); // ok - inside deprecated class

    /** y.
     * @deprecated . */
    int y = calcx();
}

/** X.
 * @deprecated . */
class X {}

class Y extends X {} // ok - not overriding anything

/** Z.
 * @deprecated . */
class Z extends X {}
