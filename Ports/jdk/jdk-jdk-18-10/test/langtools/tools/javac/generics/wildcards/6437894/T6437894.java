/*
 * @test  /nodynamiccopyright/
 * @bug     6437894
 * @summary Javac throws a NullPointerException
 * @author  jan.lahoda@...
 * @author  Peter von der Ah\u00e9
 * @compile A.java B.java
 * @clean   a.A
 * @compile/fail/ref=T6437894.out -XDrawDiagnostics T6437894.java
 */

public class T6437894 {
    public static void main(String... args) {
        b.B m;
        a.A.X x = null;
        Long.parseLong(x.toString());
    }
}
