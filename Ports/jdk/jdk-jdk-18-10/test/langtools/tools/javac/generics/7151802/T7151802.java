/*
 * @test /nodynamiccopyright/
 * @bug     7151802
 * @summary compiler update caused sqe test failed
 * @compile/fail/ref=T7151802.out -Werror -Xlint:unchecked -XDrawDiagnostics T7151802.java
 */
class T7151802 {
    static class Foo<X> { }

    static class SubFoo<X> extends Foo<X> { }

    //generic - bound - arg - non-slilent
    <Z extends Foo<String>> void get1(Z fz) { }
    void test1(Foo foo) { get1(foo); }

    //generic - bound - arg - silent
    <Z extends Foo<?>> void get2(Z fz) { }
    void test2(Foo foo) { get2(foo); }

    //generic - nobound - arg - non-slilent
    <Z> void get3(Foo<Z> fz) { }
    void test(Foo foo) { get3(foo); }

    //generic - nobound - arg - slilent
    <Z> void get4(Foo<?> fz) { }
    void test4(Foo foo) { get4(foo); }

    //generic - bound - ret - non-slilent
    <Z extends Foo<String>> Z get5() { return null; }
    void test5() { SubFoo sf = get5(); }

    //generic - bound - ret - slilent
    static <Z extends Foo<?>> Z get6() { return null; }
    void test6() { SubFoo sf = get6(); }

    //nogeneric - nobound - arg - non-slilent
    void get7(Foo<String> fz) { }
    void test7(Foo foo) { get7(foo); }

    //nogeneric - nobound - arg - slilent
    static void get8(Foo<?> fz) { }
    void test8(Foo foo) { get8(foo); }
}
