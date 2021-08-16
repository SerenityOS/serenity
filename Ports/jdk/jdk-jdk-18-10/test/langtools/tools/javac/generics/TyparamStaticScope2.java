/*
 * @test /nodynamiccopyright/
 * @bug 5046972
 * @summary type parameter referenced in static inner class improperly allowed!
 * @author gafter
 *
 * @compile/fail/ref=TyparamStaticScope2.out -XDrawDiagnostics  TyparamStaticScope2.java
 */

package typaram.static_.scope2;

class JBug<T> {
    static class Inner1 implements Set<T> {}
}

interface Set<T> {}
