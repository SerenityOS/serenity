/*
 * @test /nodynamiccopyright/
 * @bug     6677785
 * @summary REGRESSION: StackOverFlowError with Cyclic Class level Type Parameters when used in constructors
 * @author Maurizio Cimadamore
 * @compile/fail/ref=T6677785.out -XDrawDiagnostics T6677785.java
 */
public class T6677785<E extends T, T extends E> {
     T6677785() {}
     T6677785(E e) {}
     T6677785(E e, T t) {}
}
