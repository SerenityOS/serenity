/**
 * @test  /nodynamiccopyright/
 * @bug 8065219
 * @summary Deprecated warning in method reference are missing in some cases.
 * @compile/ref=DeprecationSE8Test.noLint.out -XDrawDiagnostics DeprecationSE8Test.java
 * @compile/ref=DeprecationSE8Test.out -Xlint:deprecation,-options -source 8 -XDrawDiagnostics DeprecationSE8Test.java
 */


class DeprecationSE8Test {
    @FunctionalInterface
    interface I {
        DeprecationSE8Test meth();
    }

    @FunctionalInterface
    interface J {
        int meth();
    }

    @Deprecated
    public DeprecationSE8Test() {
    }

    @Deprecated
    public static int foo() {
        return 1;
    }

    // Using deprecated entities from within one's own top level class does not merit warning.
    void notBadUsages() {
        I i = DeprecationSE8Test::new;
        new DeprecationSE8Test();
        J j = DeprecationSE8Test::foo;
        foo();
    }
}

class DeprecationSE8_01 {
    // Using deprecated entities from outside one's own top level class deserves warning.
    void badUsages() {
        DeprecationSE8Test.I i = DeprecationSE8Test::new;
        new DeprecationSE8Test();
        DeprecationSE8Test.foo();
        DeprecationSE8Test.J j = DeprecationSE8Test::foo;
    }
}
