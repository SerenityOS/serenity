/*
 * @test    /nodynamiccopyright/
 * @bug     6359949 8068836
 * @summary (at)Override of static methods shouldn't be accepted (compiler should issue an error)
 * @compile/fail/ref=T6359949a.out -XDrawDiagnostics  T6359949a.java
 */

class Example {
    public static void example() {

    }
}

class Test extends Example {
    @Override
    public static void example() {

    }
}
