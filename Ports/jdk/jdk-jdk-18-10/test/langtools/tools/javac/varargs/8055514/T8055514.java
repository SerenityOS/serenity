/*
 * @test /nodynamiccopyright/
 * @bug     8055514
 * @summary  Wrong, confusing error when non-static varargs referenced in static context
 * @compile/fail/ref=T8055514.out -Xlint:varargs -Werror -XDrawDiagnostics T8055514.java
 */
class T8055514 {
    void m(int... args) { }

    void m2(int... args) { }
    static void m2(String s) { }

    void m3(int... args) { }
    static void m3(String s) { }
    static void m3(Runnable r) { }

    void m4(int... args) { }
    void m4(int i1, int i2, int i3) { }

    static void test() {
        m(1,2,3); //only one candidate (varargs) - varargs error wins
        m2(1,2,3); //two candidates - only one applicable (varargs) - varargs error wins
        m3(1,2,3); //three candidates - only one applicable (varargs) - varargs error wins
        m4(1,2,3); //two candidates - both applicable - basic error wins
    }
}
