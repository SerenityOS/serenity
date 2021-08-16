/*
 * @test /nodynamiccopyright/
 * @bug 4901268
 * @summary JSR175 (5): annotations must be "constants"
 * @author gafter
 *
 * @compile/fail/ref=Constant.out -XDrawDiagnostics  Constant.java
 */

package Constant;

@T(a = X.x)
@interface T {
    int a();
}

class X {
    static int x;
}
