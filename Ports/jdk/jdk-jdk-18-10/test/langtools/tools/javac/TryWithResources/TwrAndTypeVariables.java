/*
 * @test /nodynamiccopyright/
 * @bug 7196163
 * @summary Twr with resource variables of parametrized types
 * @compile/fail/ref=TwrAndTypeVariables.out -XDrawDiagnostics TwrAndTypeVariables.java
 */

public class TwrAndTypeVariables {

    // positive
    public static <S extends Readable & AutoCloseable,
                   T extends Appendable & AutoCloseable>
    void copy(S s, T t) throws Exception {
        try (s; t;) {
        }
    }

    // negative
    public static <S> void copy(S s) throws Exception {
        try (s) {
        }
    }
}
