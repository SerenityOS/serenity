/*
 * @test /nodynamiccopyright/
 * @bug 4110534
 * @summary The fix for 1240831 broke the compiler.  It should
 *          report that class Abs cannot be instantiated.
 * @author turnidge
 *
 * @compile/fail/ref=ClassIsAbstract.out -XDrawDiagnostics  ClassIsAbstract.java
 */

abstract class Abs {
}

class ClassIsAbstract {
    void method() {
        new Abs();
    }
}
