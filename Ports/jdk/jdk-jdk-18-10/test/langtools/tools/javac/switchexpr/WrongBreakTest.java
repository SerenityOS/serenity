/*
 * @test /nodynamiccopyright/
 * @bug 8223305
 * @summary Ensure javac is not crashing for wrong breaks.
 * @compile/fail/ref=WrongBreakTest.out -XDrawDiagnostics -XDshould-stop.at=FLOW WrongBreakTest.java
 */

public class WrongBreakTest {

    void test(int i) {
        int s = 0;
        int j = switch (s) { default: break; };
        test(switch (s) { default: yield; });
        Runnable r = () -> {
            yield 15;
        };
        while (true) {
            yield 15;
        }
    }

    void test(Object o) {}
}
