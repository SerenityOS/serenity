/*
 * @test /nodynamiccopyright/
 * @bug 8059921
 * @summary Missing compile error in Java 8 mode for Interface.super.field access
 * @compile/fail/ref=ForbidAccessToFieldUsingSuperTest.out -XDrawDiagnostics ForbidAccessToFieldUsingSuperTest.java
 */

public class ForbidAccessToFieldUsingSuperTest {
    class C {
        int m() { return 0; }
    }

    interface T {
        int f = 0;
        C c = null;
        default int mm() {
            return 0;
        }
    }

    interface T1 extends T {}

    class X implements T1 {
        int i = T1.super.f;        //fail
        int j = T1.super.c.m();    //fail

        void foo(Runnable r) {
            foo(T1.super::mm);     //should'n fail
        }
    }
}
