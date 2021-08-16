/*
 * @test /nodynamiccopyright/
 * @bug 4821359
 * @summary Add -Xlint flag
 * @author gafter
 *
 * @compile/fail/ref=Unchecked.out -XDrawDiagnostics  -Xlint:unchecked -Werror Unchecked.java
 */

class Unchecked<T> {
    void f(Unchecked u) {
        Unchecked<String> us = u;
    }
}
