/*
 * @test /nodynamiccopyright/
 * @bug 4881265
 * @summary generics: compiler allows T.class for type variable T
 * @author gafter
 *
 * @compile/fail/ref=TyparamLit.out -XDrawDiagnostics  TyparamLit.java
 */

class TyparamLit<T> {
    Class x = T.class;
}
