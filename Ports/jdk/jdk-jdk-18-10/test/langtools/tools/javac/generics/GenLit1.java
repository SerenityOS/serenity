/*
 * @test /nodynamiccopyright/
 * @bug 4987844
 * @summary compiler crash with ill-formed annotation
 * @author gafter
 *
 * @compile/fail/ref=GenLit1.out -XDrawDiagnostics   GenLit1.java
 */

package genLit1;

@interface X {
    Class value();
}

@X(Z.class)
class Y<Z> {
}
