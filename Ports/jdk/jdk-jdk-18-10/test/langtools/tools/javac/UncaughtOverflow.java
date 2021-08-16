/*
 * @test /nodynamiccopyright/
 * @bug 4035346 4097402
 * @summary Compiler used to allow this initialization, despite the overflow.
 * @author turnidge
 *
 * @compile/fail/ref=UncaughtOverflow.out -XDrawDiagnostics UncaughtOverflow.java
 */

public
class UncaughtOverflow {
    int i1 = 100000000000;
    int i2 = -2147483649;
}
