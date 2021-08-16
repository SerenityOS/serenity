/*
 * @test /nodynamiccopyright/
 * @bug 7086595
 * @summary Error message bug: name of initializer is 'null'
 * @compile/fail/ref=T7086595.out -XDrawDiagnostics T7086595.java
 */

class T7086595 {

    String s = "x";
    String s = nonExistent;

    int foo() {
        String s = "x";
        String s = nonExistent;
    }

    static int bar() {
        String s = "x";
        String s = nonExistent;
    }

    {
        String s = "x";
        String s = nonExistent;
    }

    static {
        String s = "x";
        String s = nonExistent;
    }
}
