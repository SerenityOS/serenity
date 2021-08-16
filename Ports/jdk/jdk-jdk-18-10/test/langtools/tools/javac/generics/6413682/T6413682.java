/*
 * @test    /nodynamiccopyright/
 * @bug     6413682
 * @summary Compiler confused about implicit type args and arrays
 * @compile/fail/ref=T6413682.out -XDrawDiagnostics  T6413682.java
 */

public class T6413682 {
    public static void main(String... args) {
        Object[] text = new
<Wow,thanks.Mr<List>,and.thanks.Mr<Javac>.Vince>Object[5];
    }
}
