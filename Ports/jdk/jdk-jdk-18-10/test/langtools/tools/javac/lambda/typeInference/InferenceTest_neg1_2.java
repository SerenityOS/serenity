/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   Overloaded methods take raw SAM types that have type inference according to SAM descriptor
             should have ambiguous resolution of method
 * @compile/fail/ref=InferenceTest_neg1_2.out -XDrawDiagnostics InferenceTest_neg1_2.java
 */

public class InferenceTest_neg1_2 {

    public static void main(String[] args) {
        InferenceTest_neg1_2 test = new InferenceTest_neg1_2();
        test.method(n -> null); //method 1-5 all match
        test.method(n -> "a"); //method 2, 4 match
        test.method(n -> 0); //method 1, 3, 5 match
    }

    void method(SAM1 s) { //method 1
        Integer i = s.foo("a");
    }

    void method(SAM2 s) { //method 2
        String str = s.foo(0);
    }

    void method(SAM3<Integer> s) { //method 3
        Integer i = s.get(0);
    }

    void method(SAM4<Double, String> s) { //method 4
        String str = s.get(0.0);
    }

    void method(SAM5<Integer> s) { //method 5
        Integer i = s.get(0.0);
    }

    interface SAM1 {
        Integer foo(String a);
    }

    interface SAM2 {
        String foo(Integer a);
    }

    interface SAM3<T> {
        T get(T t);
    }

    interface SAM4<T, V> {
        V get(T t);
    }

    interface SAM5<T> {
        T get(Double i);
    }
}
