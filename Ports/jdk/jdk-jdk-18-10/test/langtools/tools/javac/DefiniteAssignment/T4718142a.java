/*
 * @test /nodynamiccopyright/
 * @bug 4718142
 * @summary DU analysis not conservative for try-finally
 * @author Neal Gafter (gafter)
 *
 * @compile/fail/ref=T4718142a.out -XDrawDiagnostics  T4718142a.java
 */

class T4718142a {
    public static void main(String[] args) {
        final int i;
        for (int n=0; n<10; n++) {
            b: {
                try {
                    if (true) break b;
                } finally {
                    i = n;
                    System.out.println("i = " + i);
                }
                return;
            }
        }
    }
}
