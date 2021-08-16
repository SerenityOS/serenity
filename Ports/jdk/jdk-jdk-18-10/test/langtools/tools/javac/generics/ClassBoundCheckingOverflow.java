/*
 * @test /nodynamiccopyright/
 * @bug 8214345
 * @summary infinite recursion while checking super class
 *
 * @compile/fail/ref=ClassBoundCheckingOverflow.out -XDrawDiagnostics ClassBoundCheckingOverflow.java
 */

public class ClassBoundCheckingOverflow {
    abstract class InfiniteLoop1<E extends InfiniteLoop1<E>> extends E {}
    abstract class InfiniteLoop2<E extends InfiniteLoop2<E>> implements E {}
}
