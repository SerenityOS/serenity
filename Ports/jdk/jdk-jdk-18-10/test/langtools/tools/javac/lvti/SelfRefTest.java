/*
 * @test /nodynamiccopyright/
 * @bug 8177466
 * @summary Add compiler support for local variable type-inference
 * @compile/fail/ref=SelfRefTest.out -XDrawDiagnostics SelfRefTest.java
 */

import java.util.function.Function;

class SelfRefTest {

        int q() { return 42; }
    int m(int t) { return t; }

    void test(boolean cond) {
       var x = cond ? x : x; //error - self reference
       var y = (Function<Integer, Integer>)(Integer y) -> y; //error - bad shadowing
       var z = (Runnable)() -> { int z2 = m(z); }; //error - self reference
       var w = new Object() { int w = 42; void test() { int w2 = w; } }; //ok
       int u = u; //ok
       int q = q(); //ok
    }
}
