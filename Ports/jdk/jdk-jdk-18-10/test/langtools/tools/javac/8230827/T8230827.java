/*
 * @test /nodynamiccopyright/
 * @bug 8230827
 * @summary javac gives inappropriate warning about potentially ambiguous methods
 * @compile/fail/ref=T8230827.out -XDrawDiagnostics -Xlint:all -Werror T8230827.java
 */

class T8230827 {
    interface I1 {
        void m1(int i);
    }

    interface I2 {
        void m2(boolean b);
    }

    public void nonambiguousMethod1(Boolean differentParam, I1 ambiguousInterface) {}

    public void nonambiguousMethod1(String differentParam, I2 ambiguousInterface) {}


    public void nonambiguousMethod2(Object ambiguousParam, I1 ambiguousInterface, String differentParam) {}

    public void nonambiguousMethod2(Object ambiguousParam, I2 ambiguousInterface, Boolean differentParam) {}


    public void ambiguousMethod1(Object ambiguousParam, I1 ambiguousInterface) {}

    public void ambiguousMethod1(Object ambiguousParam, I2 ambiguousInterface) {}


    public void ambiguousMethod2(I1 ambiguousInterface, Object ambiguousParam) {}

    public void ambiguousMethod2(I2 ambiguousInterface, Object ambiguousParam) {}


    public void ambiguousMethod3(I1 ambiguousInterface, I1 sameInterface) {}

    public void ambiguousMethod3(I2 ambiguousInterface, I1 sameInterface) {}


    public void ambiguousMethod4(Object ambiguousParent, I1 ambiguousInterface, String ambiguousChild) {}

    public void ambiguousMethod4(String ambiguousChild, I2 ambiguousInterface, Object ambiguousParent) {}
}
