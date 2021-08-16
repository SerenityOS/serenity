/*
 * @test    /nodynamiccopyright/
 * @bug     4041948
 * @summary javac previously allowed interfaces to inherit methods with
 *          inconsistent return types.
 * @author  turnidge
 *
 * @compile/fail/ref=InconsistentReturn.out -XDrawDiagnostics  InconsistentReturn.java
 */
interface I1{
  int f();
}
interface I2 {
  void f() ;
}
// error: Return types conflict.
interface InconsistentReturn extends I1,I2 { }
