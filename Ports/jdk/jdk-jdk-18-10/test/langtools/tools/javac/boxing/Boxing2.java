/*
 * @test /nodynamiccopyright/
 * @bug 4974939
 * @summary Boxing/unboxing negative unit and regression tests
 * @author gafter
 * @compile/fail/ref=Boxing2.out -XDrawDiagnostics  Boxing2.java
 */

public class Boxing2 {

    void f() {
        Long l = 12; // no compound boxing
    }
}
