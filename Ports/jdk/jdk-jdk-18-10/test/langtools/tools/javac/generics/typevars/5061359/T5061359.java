/*
 * @test    /nodynamiccopyright/
 * @bug     5061359
 * @summary No error for ambiguous member of intersection
 * @compile/fail/ref=T5061359.out -XDrawDiagnostics  T5061359.java
 */

class Test<T extends Base & Intf> {
    public void foo() {
        T t = null;
        T.Inner inner = null; // This should be an ambiguous error
    }

}

class Base {
    static class Inner {}
}

interface Intf {
    class Inner {}
}
