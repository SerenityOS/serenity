/**
 * @test    /nodynamiccopyright/
 * @bug     8044196
 * @summary Ensure that a broken type annotation container generates a correct error message.
 * @compile T.java TC.java
 * @compile TCBroken.java
 * @compile/fail/ref=BrokenTypeAnnoContainer.out -XDrawDiagnostics BrokenTypeAnnoContainer.java
 */

class BrokenTypeAnnoContainer {
    void method() {
        int ll2 = (@T(1) @T(2) int) 0;
    }
}
