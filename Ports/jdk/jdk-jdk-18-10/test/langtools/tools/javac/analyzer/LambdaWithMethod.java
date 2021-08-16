/**
 * @test /nodynamiccopyright/
 * @bug 8191981
 * @compile/fail/ref=LambdaWithMethod.out -Werror -XDrawDiagnostics -XDfind=lambda LambdaWithMethod.java
 */

public class LambdaWithMethod {
    public static void run(Runnable r) {
        run(new Runnable() {
            public void run() {
                put(get());
            }
        });
    }
    private static String get() { return null; }
    private static void put(String i) {}
}
