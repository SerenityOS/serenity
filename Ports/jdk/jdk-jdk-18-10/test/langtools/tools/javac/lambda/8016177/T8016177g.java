/*
 * @test /nodynamiccopyright/
 * @bug 8016081 8016178 8069545 8078024
 * @summary structural most specific and stuckness
 * @compile/fail/ref=T8016177g.out -XDrawDiagnostics T8016177g.java
 */


class Test {

    interface Function<X, Y> {
        Y m(X x);
    }

    interface Box<T> {
        T get();
        <R> R map(Function<T,R> f);
    }

    static class Person {
        Person(String name) { }
    }

    void print(Object arg) { }
    void print(String arg) { }

    int abs(int a) { return 0; }
    long abs(long a) { return 0; }
    float abs(float a) { return 0; }
    double abs(double a) { return 0; }

    void test() {
        Box<String> b = null;
        print(b.map(s -> new Person(s)));
        int i = abs(b.map(s -> Double.valueOf(s)));
    }
}
