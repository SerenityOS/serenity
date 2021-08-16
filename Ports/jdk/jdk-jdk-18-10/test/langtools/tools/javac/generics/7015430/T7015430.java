/*
 * @test /nodynamiccopyright/
 * @bug 7015430
 *
 * @summary  Incorrect thrown type determined for unchecked invocations
 * @author Daniel Smith
 * @compile/fail/ref=T7015430_1.out -source 7 -Xlint:-options,unchecked -XDrawDiagnostics T7015430.java
 * @compile/fail/ref=T7015430_2.out -Xlint:unchecked -XDrawDiagnostics T7015430.java
 *
 */

class T7015430 {
    static <E extends Exception> Iterable<E> empty(Iterable<E> arg) throws E {
        return null;
    }

    <E extends Exception> T7015430(Iterable<E> arg) throws E { }

    static <E extends Exception> Iterable<E> empty2(Iterable x) throws E {
        return null;
    }

    static class Foo<X extends Exception> {
        Foo() throws X {}
    }

    /**
    * Method invocation, no unchecked
    * inferred: RuntimeException - should pass
    */
    void m1() {
        Iterable<RuntimeException> i = java.util.Collections.emptyList();
        empty(i);
    }

    /**
    * Method invocation, unchecked, inferred arguments
    * inferred: Exception - should fail
    */
    void m2() {
        Iterable i = java.util.Collections.EMPTY_LIST;
        empty(i);
    }

    /**
    * Method invocation, unchecked, explicit arguments
    * inferred: RuntimeException - should pass
    */
    void m3() {
        Iterable i = java.util.Collections.EMPTY_LIST;
        T7015430.<RuntimeException>empty(i);
    }

    /**
    * Constructor invocation, no unchecked
    * inferred: RuntimeException - should pass
    */
    void m4() {
        Iterable<RuntimeException> i = java.util.Collections.emptyList();
        new T7015430(i);
    }

    /**
    * Constructor invocation, unchecked, inferred arguments
    * inferred: Exception - should fail
    */
    void m5() {
        Iterable i = java.util.Collections.EMPTY_LIST;
        new T7015430(i);
    }

    /**
    * Constructor invocation, unchecked, explicit arguments
    * inferred: RuntimeException - should pass
    */
    void m6() {
        Iterable i = java.util.Collections.EMPTY_LIST;
        new <RuntimeException>T7015430(i);
    }

    /**
    * Method invocation, no unchecked, inferred arguments
    * inferred: RuntimeException - should pass
    */
    void m7() {
        Iterable i = java.util.Collections.EMPTY_LIST;
        Iterable<RuntimeException> e = empty2(i);
    }

    /**
    * Method invocation, no unchecked, inferred arguments
    * inferred: Exception - should fail
    */
    void m8() {
        Iterable i = java.util.Collections.EMPTY_LIST;
        empty2(i);
    }

    /**
    * Constructor invocation, unchecked, explicit arguments
    * inferred: RuntimeException - should pass
    */
    void m9() {
        Iterable i = java.util.Collections.EMPTY_LIST;
        new <RuntimeException> T7015430(i);
    }

    /**
    * Constructor invocation, unchecked, inferred arguments
    * inferred: Exception - should fail
    */
    void m10() {
        Iterable i = java.util.Collections.EMPTY_LIST;
        new T7015430(i);
    }

    /**
    * Constructor invocation, no unchecked, inferred arguments (diamond)
    * inferred: RuntimeException - should pass
    */
    void m11() {
        Foo<RuntimeException>  o = new Foo<>();
    }

    /**
    * Constructor invocation, no unchecked, inferred arguments (diamond)
    * inferred: Exception - should fail
    */
    void m12() {
        new Foo<>();
    }
}
