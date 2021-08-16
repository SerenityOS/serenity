/*
 * @test /nodynamiccopyright/
 * @bug 4718142
 * @summary DU analysis not conservative for try-finally
 * @author Neal Gafter (gafter)
 *
 * @compile/fail/ref=T4718142.out -XDrawDiagnostics  T4718142.java
 */

class T4718142 {
    static class E extends Exception {}
    static void thr() throws E {
        throw new E();
    }
    public static void main(String[] args) {
        int count = 0;
        final int i;
        while (true) {
            try {
                i = count++;
                System.out.println("assigned " + i);
                thr();
                while (true) {}
            } catch (E e) {}
        }
    }
}
