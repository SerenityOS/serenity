/*
 * @test /nodynamiccopyright/
 * @bug 8081769
 * @summary Redundant error message on bad usage of 'class' literal
 * @compile/fail/ref=T8081769.out -XDrawDiagnostics T8081769.java
 */
class T8081769 {
    void test() {
        Class c1 = this.class;
        Class c2 = "".class;
        Class c3 = 0 .class;
        Class c4 = null.class;
        Object x;
        Class c5 = x.toString().class;
    }
}
