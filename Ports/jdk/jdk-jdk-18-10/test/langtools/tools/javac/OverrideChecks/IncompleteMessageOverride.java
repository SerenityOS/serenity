/*
 * @test  /nodynamiccopyright/
 * @bug 8035956
 * @summary javac, incomplete error message
 * @author kizune
 *
 * @run compile/fail/ref=IncompleteMessageOverride.out -XDrawDiagnostics IncompleteMessageOverride.java
 */

class Super {
    static void m() {}
}

class Sub extends Super {
    private static void m() {}
}
