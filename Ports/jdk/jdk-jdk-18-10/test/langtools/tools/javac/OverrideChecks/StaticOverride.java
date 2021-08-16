/*
 * @test    /nodynamiccopyright/
 * @bug     4041948 4022450
 * @summary javac previously allowed static methods to override non-static
 *          methods in some cases.
 * @author  turnidge
 *
 * @compile/fail/ref=StaticOverride.out -XDrawDiagnostics  StaticOverride.java
 */
interface I{
  int f();
}

class C {
    public static int f() {
        return 7;
    }
}

class StaticOverride extends C implements I { }
