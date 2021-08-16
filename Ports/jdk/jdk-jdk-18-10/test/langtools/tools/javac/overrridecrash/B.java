/*
 * @test /nodynamiccopyright/
 * @bug 4909690
 * @summary AssertionError(com.sun.tools.javac.v8.code.Symbol$MethodSymbol.isOverridableIn)
 * @author gafter
 *
 * @compile/fail/ref=B.out -XDrawDiagnostics B.java
 */

public class B extends A
{
    private protected int m() { return 0; }
}
