/*
 * @test /nodynamiccopyright/
 * @bug 4041948
 * @summary javac previously allowed interfaces to inherit methods with
 *          inconsistent signatures.
 * @author turnidge
 *
 * @compile/fail/ref=InconsistentInheritedSignature.out -XDrawDiagnostics InconsistentInheritedSignature.java
 */
interface I1{
  int f();
}
interface I2 {
  void f() ;
}
// error: Return types conflict.
interface InconsistentInheritedSignature extends I1,I2 { }
