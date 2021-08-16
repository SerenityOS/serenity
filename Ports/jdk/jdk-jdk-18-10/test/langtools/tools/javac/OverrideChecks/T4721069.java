/*
 * @test    /nodynamiccopyright/
 * @bug     4721069
 * @summary javac allows an interface to override a final method in Object
 * @author  gafter
 *
 * @compile/fail/ref=T4721069.out -XDrawDiagnostics  T4721069.java
 */

interface I {
    Class getClass(); // error: cannot overide final from Object
    static class T {
        static void f(I i) {
            if (i == null) {
                Integer x = Integer.valueOf(2);
            } else {
                I x = i;
                x.getClass();
            }
        }
        public static void main(String[] args) {
            f(null);
        }
    }
}
