/*
 * @test /nodynamiccopyright/
 * @bug 4989667
 * @summary Annotation members must not have same signature as Object or Annotation members
 * @author gafter
 *
 * @compile/fail/ref=MemberOver.out -XDrawDiagnostics  MemberOver.java
 */

package memberOver;

@interface T {
    int hashCode();
}
