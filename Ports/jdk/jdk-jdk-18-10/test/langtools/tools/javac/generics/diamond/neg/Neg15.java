/*
 * @test /nodynamiccopyright/
 * @bug 8062373 8151018
 *
 * @summary  Test that javac complains when a <> inferred class contains a public method that does override a supertype method.
 * @author sadayapalam
 * @compile/fail/ref=Neg15.out Neg15.java -XDrawDiagnostics
 *
 */

class Neg15 {

    interface Predicate<T> {
        default boolean test(T t) {
            System.out.println("Default method");
            return false;
        }
    }


    static void someMethod(Predicate<? extends Number> p) {
        if (!p.test(null))
            throw new Error("Blew it");
    }

    public static void main(String[] args) {

        someMethod(new Predicate<Integer>() {
            public boolean test(Integer n) {
                System.out.println("Override");
                return true;
            }
            boolean test(Integer n, int i) {
                System.out.println("Override");
                return true;
            }
            protected boolean test(Integer n, int i, int j) {
                System.out.println("Override");
                return true;
            }
            private boolean test(Integer n, int i, long j) {
                System.out.println("Override");
                return true;
            }
        });

        someMethod(new Predicate<>() {
            public boolean test(Integer n) { // bad.
                System.out.println("Override");
                return true;
            }
            boolean test(Integer n, int i) { // bad, package access.
                System.out.println("Override");
                return true;
            }
            protected boolean test(Integer n, int i, int j) { // bad, protected access.
                System.out.println("Override");
                return true;
            }
            private boolean test(Integer n, int i, long j) { // OK, private method.
                System.out.println("Override");
                return true;
            }
        });
    }
}
