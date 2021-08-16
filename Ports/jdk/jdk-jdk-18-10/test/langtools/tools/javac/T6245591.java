/*
 * @test  /nodynamiccopyright/
 * @bug 6245591
 * @compile/ref=T6245591.out -XDrawDiagnostics -Xlint:all,-path T6245591.java
 */
enum Season {
    /** @deprecated */
    WINTER, SPRING, SUMMER, FALL;
}
enum Season1 {
    WINTER, SPRING, SUMMER, FALL;
}
class T6245591 {
    void m() {
        Season s1 = Season.WINTER;    // ref to WINTER should be deprecated
        Season1 s2 = Season1.WINTER;  // nothing here should be deprecated;
                                      // @deprecated should not leak out of Season
    }
}
