/*
 * @test /nodynamiccopyright/
 * @bug 8062373
 * @summary Test that javac does not recommend a diamond site that would result in error.
 * @compile/ref=Neg16.out -Xlint:-options Neg16.java -XDrawDiagnostics -XDfind=diamond
 */

class Neg16 {

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
        someMethod(new Predicate<Integer>() { // cannot convert to diamond
            public boolean test(Integer n) {
                System.out.println("Override");
                return true;
            }
        });
        someMethod(new Predicate<Number>() { // can convert to diamond.
            public boolean test(Number n) {
                System.out.println("Override");
                return true;
            }
        });
    }
}
