/*
 * @test /nodynamiccopyright/
 * @bug 4984157
 * @summary java.lang.Throwable inheritance in parameterized type
 * @author gafter
 *
 * @compile/fail/ref=GenericThrowable.out -XDrawDiagnostics   GenericThrowable.java
 */

class GenericThrowable<T> extends NullPointerException {
}
