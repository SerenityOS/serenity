/*
 * @test /nodynamiccopyright/
 * @bug 4049982 8056897
 * @summary Compiler permitted invalid hex literal.
 * @author turnidge
 *
 * @compile/fail/ref=BadHexConstant.out -XDrawDiagnostics  BadHexConstant.java
 */

public
class BadHexConstant {
    long i = 0xL;
}
