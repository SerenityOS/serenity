/*
 * @test /nodynamiccopyright/
 * @bug 8231827
 * @summary Pattern variables are non-final.
 * @compile/fail/ref=PatternVariablesAreNonFinal.out -XDrawDiagnostics PatternVariablesAreNonFinal.java
 */
public class PatternVariablesAreNonFinal {
    public static void main(String[] args) {
        Object o = 32;
        if (o instanceof String s) {
            s = "hello again";
            new Runnable() {
                @Override
                public void run() {
                    System.err.println(s);
                }
            };
        }
        System.out.println("test complete");
    }
}
