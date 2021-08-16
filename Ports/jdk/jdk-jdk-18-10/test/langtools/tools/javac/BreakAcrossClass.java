/*
 * @test /nodynamiccopyright/
 * @bug 4407643
 * @summary javac throws NullPointerException for break to label outside of class
 * @author gafter
 *
 * @compile/fail/ref=BreakAcrossClass.out -XDrawDiagnostics  BreakAcrossClass.java
 */

class BreakAcrossClass {
     public static void main(String argv[]) {
        final int i = 6;
    M:  {
            class A {
                {
                    if (i != 5) break M;
                }
            }
            System.out.println("failed : " + i);
        }
        System.out.println("passed : " + i);
    }
}
