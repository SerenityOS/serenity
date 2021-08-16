/*
 * @test  /nodynamiccopyright/
 * @bug 5017953
 * @summary spurious cascaded diagnostics when name not found
 * @compile/fail/ref=T5017953.out -XDrawDiagnostics T5017953.java
 */

class T5017953 {

    int f = 0;
    void test(int i) {}

    {   test(NonExistentClass.f ++);
        test(1 + NonExistentClass.f);
        test(NonExistentClass.f + 1);
        test(NonExistentClass.f + NonExistentClass.f);
        test(NonExistentClass.f += 1);
        test(f += NonExistentClass.f);
    }
}
