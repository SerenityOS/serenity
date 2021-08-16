/*
 * @test /nodynamiccopyright/
 * @bug 5057445
 * @summary javac allows catching type parameter
 * @author gafter
 *
 * @compile/fail/ref=CatchTyparam.out -XDrawDiagnostics  CatchTyparam.java
 */

class J {
    <T extends Error, U extends Error> void foo() {
        try {
            int i = 12;
        } catch (T ex) {
        } catch (U ex) {
        }
    }
}
