/*
 * @test /nodynamiccopyright/
 * @bug 8062373
 *
 * @summary  Test diamond + anonymous classes with super type being an interface.
 * @author sadayapalam
 * @compile/fail/ref=Neg14.out Neg14.java -XDrawDiagnostics
 *
 */
class Neg14 {

    static interface A<T> {
        void foo();
    }

    static void foo(A<String> as) {}

    public static void main(String[] args) {

        // Method invocation context - good <>(){}
        foo(new A<>() {
            public void foo() {}
        });

        // Assignment context - good <>(){}
        A<?> aq = new A<>() {
            public void foo() {}
        };

        // When the anonymous type subtypes an interface but is missing definitions for
        // abstract methods, expect no overload resolution error, but an attribution error
        // while attributing anonymous class body.


        // Method invocation context - bad <>(){}
        foo(new A<>() {
        });

        // Assignment invocation context - bad <>(){}
        aq = new A<>() {
        };

        // Method invocation context - bad <>()
        foo(new A<>());

        // Assignment invocation context - bad <>()
        aq = new A<>();
    }
}
