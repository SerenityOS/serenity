/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that only SAM are allowed as target types for lambda expressions
 * @author Jan Lahoda
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=BadTargetType.out -XDrawDiagnostics BadTargetType.java
 */

class BadTargetType {

    static void m1(Object o) {}
    void m2(Object o) {}

    static Object l1 = (int pos)-> { };
    Object l2 = (int pos)-> { };

    {
        m1((int pos)-> { });
        m2((int pos)-> { });
    }
}
