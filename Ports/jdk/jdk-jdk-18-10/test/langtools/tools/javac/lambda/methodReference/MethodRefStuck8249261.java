/*
 * @test /nodynamiccopyright/
 * @bug 8249261
 * @summary Verify method references as conditions to conditional expressions
 *          are handled properly.
 * @compile/fail/ref=MethodRefStuck8249261.out -XDrawDiagnostics MethodRefStuck8249261.java
 */
class MethodRefStuck8249261 {

    void p(int padding) {}

    static boolean t() {
        return true;
    }

    private void test() {
        p(MethodRefStuck8249261::t);
        p((MethodRefStuck8249261::t));
        p(MethodRefStuck8249261::t + 1);
        p(MethodRefStuck8249261::t ? 1 : 0);
        p(true ? MethodRefStuck8249261::t : 0);
        p(switch (MethodRefStuck8249261::t) { default -> 0; });
        p(() -> true);
        p((() -> true));
        p((() -> true) + 1);
        p((() -> true) ? 1 : 0);
        p(true ? (() -> true) : 0);
        p(switch ((() -> true)) { default -> 0; });
  }
}
