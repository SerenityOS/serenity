/*
 * @test /nodynamiccopyright/
 * @bug 4717164
 * @summary missing catch not reachable error when nested try-finally returns in finally
 * @author Neal Gafter (gafter)
 *
 * @compile/fail/ref=T4717164.out -XDrawDiagnostics  T4717164.java
 */

class T4717164 {
    public static void main(String[] args) {
        try {
            try {
                throw new ClassNotFoundException();
            } catch (ClassNotFoundException e) {
                throw e;
            } finally {
                return; // discards ClassNotFoundException
            }
        } catch (ClassNotFoundException e1) { // error: unreachable
        }
    }
}
