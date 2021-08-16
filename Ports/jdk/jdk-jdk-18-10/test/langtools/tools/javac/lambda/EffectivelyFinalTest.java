/*
 * @test /nodynamiccopyright/
 * @bug 7175538 8003280
 * @summary Add lambda tests
 *  Integrate effectively final check with DA/DU analysis
 * @compile/fail/ref=EffectivelyFinalTest01.out -XDrawDiagnostics EffectivelyFinalTest.java
 * @compile/fail/ref=EffectivelyFinalTest02.out -source 7 -Xlint:-options -XDrawDiagnostics EffectivelyFinalTest.java
 */
class EffectivelyFinalTest {

    void m1(int x) {
        int y = 1;
        new Object() { { System.out.println(x+y); } }; //ok - both x and y are EF
    }

    void m2(int x) {
        int y;
        y = 1;
        new Object() { { System.out.println(x+y); } }; //ok - both x and y are EF
    }

    void m3(int x, boolean cond) {
        int y;
        if (cond) y = 1;
        new Object() { { System.out.println(x+y); } }; //error - y not DA
    }

    void m4(int x, boolean cond) {
        int y;
        if (cond) y = 1;
        else y = 2;
        new Object() { { System.out.println(x+y); } }; //ok - both x and y are EF
    }

    void m5(int x, boolean cond) {
        int y;
        if (cond) y = 1;
        y = 2;
        new Object() { { System.out.println(x+y); } }; //error - y not EF
    }

    void m6(int x) {
        new Object() { { System.out.println(x+1); } }; //error - x not EF
        x++; // Illegal: x is not effectively final.
    }

    void m7(int x) {
        new Object() { { System.out.println(x=1); } }; //error - x not EF
    }

    void m8() {
        int y;
        new Object() { { System.out.println(y=1); } }; //error - y not EF
    }
}
