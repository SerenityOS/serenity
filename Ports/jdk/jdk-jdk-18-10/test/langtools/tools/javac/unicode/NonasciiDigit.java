/*
 * @test /nodynamiccopyright/
 * @bug 4707960 6183529 8046620
 * @summary javac accepts unicode digits - sometimes crashing
 * @author gafter
 *
 * @compile/fail/ref=NonasciiDigit.out -XDrawDiagnostics  NonasciiDigit.java
 */
public class NonasciiDigit {
    public static void main(String[] args) {
        // error: only ASCII allowed in constants
        int i1 = \uff11;
        int i2 = 1\uff11;
        int i3 = \ud835\udfff;
        // error: floating literals use ascii only
        double d1 = \uff11.0;
        double d2 = 0.\uff11;
        double d3 = 0x0P\uff11;
        double d4 = 0E\uff11;
        double d5 = .\uff11;
        double d6 = \ud835\udfff.0;
    }
}
