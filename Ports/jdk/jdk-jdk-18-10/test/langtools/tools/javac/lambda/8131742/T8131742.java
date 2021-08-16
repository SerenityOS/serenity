/*
 * @test /nodynamiccopyright/
 * @bug 8131742
 * @summary Syntactically meaningless code accepted by javac
 * @compile/fail/ref=T8131742.out -XDrawDiagnostics T8131742.java
 */
class T8131742 {
    static Runnable r = (__GAR BAGE__.this) -> { };
}
