/*
 * @test  /nodynamiccopyright/
 * @bug 6183529
 * @summary javac gives warnings instead of errors on non-ASCII digits
 * @compile/fail/ref=Digits.out -XDrawDiagnostics Digits.java
 */

class Digits
{
    public static final double good =  1.23;
    public static final double bad  =  1.2\u0663;
}
