/*
 * @test  /nodynamiccopyright/
 * @bug     4994049
 * @summary Improved diagnostics while parsing enums
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=T4994049.out -XDrawDiagnostics T4994049.java
 */

public enum T4994049 {
    FOO {
    }

    BAR1,
    BAR2(),
    BAR3 {},
    BAR4() {};
}
