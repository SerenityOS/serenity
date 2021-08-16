/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  recovery attribution step for unchecked arguments
 * @compile/fail/ref=TargetType43.out -XDrawDiagnostics TargetType43.java
 */
class TargetType43 {

    void m(Object o) { }

    void test(Object obj) {
        Object o = x-> { new NonExistentClass(x); return 5; };
        m(x-> { new NonExistentClass(x); return 5; });
    }
}
