/*
 * @test /nodynamiccopyright/
 * @bug 4309152 4805490
 * @summary Compiler silently generates bytecode that exceeds VM limits
 * @author gafter
 *
 * @compile/fail/ref=StringLength.out -XDrawDiagnostics StringLength.java
 */

class StringLength {
    public static final String l5e0 = "abcde";
    public static final String l1e1 = l5e0 + l5e0;
    public static final String l3e1 = l1e1 + l1e1 + l1e1;
    public static final String l1e2 = l3e1 + l3e1 + l3e1 + l1e1;
    public static final String l5e2 = l1e2 + l1e2 + l1e2 + l1e2 + l1e2;
    public static final String l1e3 = l5e2 + l5e2;
    public static final String l5e3 = l1e3 + l1e3 + l1e3 + l1e3 + l1e3;
    public static final String l1e4 = l5e3 + l5e3;
    public static final String l6e4 = l1e4 + l1e4 + l1e4 + l1e4 + l1e4 + l1e4;

    public static final String l65530 = l6e4 + l5e3 + l5e2 + l3e1;
    public static String X = l65530 + "abcdef"; // length 65536
    public static void main(String[] args) {
        System.out.println(X.length());
    }
}
