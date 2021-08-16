/*
 * @test /nodynamiccopyright/
 * @bug 8021567
 * @summary Javac doesn't report "java: reference to method is ambiguous" any more
 * @compile/fail/ref=T8021567.out -XDrawDiagnostics T8021567.java
 */

class T8021567 {

    interface I_int { int m(); }

    interface I_char { char m(); }

    interface I_byte { byte m(); }

    void m(I_byte b) { }
    void m(I_char b) { }
    void m(I_int b) { }

    void test() {
        m(() -> 1); //ambiguous
        m(() -> 256); //ok - only method(I_int) applicable
        m(() -> { int i = 1; return i; }); //ok - only method(I_int) applicable
        m(() -> { int i = 256; return i; }); //ok - only method(I_int) applicable
    }
}
