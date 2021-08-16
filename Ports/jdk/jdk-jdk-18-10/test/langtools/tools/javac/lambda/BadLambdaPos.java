/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that lambda is only allowed in argument/cast/assignment context
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=BadLambdaPos.out -XDrawDiagnostics BadLambdaPos.java
 */

interface SAM {
    void m(Integer x);
}

class Test {
    void test(Object x) {}

    void test1() {
        test((int x)-> { } + (int x)-> { } );
        test((int x)-> { } instanceof Object );
    }

    void test2() {
        int i2 = (int x)-> { } + (int x)-> { };
        boolean b = (int x)-> { } instanceof Object;
    }

    void test3() {
        test((Object)(int x)-> { });
        Object o = (Object)(int x)-> { };
    }
}
