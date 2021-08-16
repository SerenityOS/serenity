/*
 * @test /nodynamiccopyright/
 * @bug 8022316
 * @summary Generic throws, overriding and method reference
 * @compile/fail/ref=CompilerErrorGenericThrowPlusMethodRefTest.out -XDrawDiagnostics CompilerErrorGenericThrowPlusMethodRefTest.java
 */

@SuppressWarnings("unchecked")
public class CompilerErrorGenericThrowPlusMethodRefTest {
    interface SAM11 {
        public <E extends Throwable> void foo() throws E ;
    }

    interface SAM12 extends SAM11{
        @Override
        public void foo() throws Throwable;
    }

    public void boo() throws RuntimeException {}

    static void test1() {
        try {
            SAM12 s2 = new CompilerErrorGenericThrowPlusMethodRefTest()::boo;
            s2.foo();
        } catch(Throwable ex) {}
    }

    static void test2() {
        SAM11 s1 = null;
        s1.<Exception>foo();
        s1.<RuntimeException>foo();
    }

    interface SAM21 {
        <E extends Exception> void m(E arg) throws E;
    }

    interface SAM22 {
        <F extends Exception> void m(F arg) throws F;
    }

    interface SAM23 extends SAM21, SAM22 {}

    public <E extends Exception> void bar(E e) throws E {}

    static <E extends Exception> void test3(E e) {
        try {
            SAM23 s2 = new CompilerErrorGenericThrowPlusMethodRefTest()::bar;
            s2.m(e);
        } catch(Exception ex) {}
    }

}
