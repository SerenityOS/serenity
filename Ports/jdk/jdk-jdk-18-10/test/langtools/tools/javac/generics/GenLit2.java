/*
 * @test /nodynamiccopyright/
 * @bug 4942201
 * @summary java allows class literal on generic type parameter array
 * @author gafter
 *
 * @compile/fail/ref=GenLit2.out -XDrawDiagnostics  GenLit2.java
 */

package genLit2;

class U<T> {
    Class t = T[].class;
}
