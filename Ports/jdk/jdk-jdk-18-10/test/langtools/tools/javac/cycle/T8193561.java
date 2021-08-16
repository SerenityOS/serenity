/*
 * @test /nodynamiccopyright/
 * @bug 8193561
 * @summary Verify that there is no crash for default methods in mutually dependent interfaces
 * @compile/fail/ref=T8193561.out -XDrawDiagnostics T8193561.java
 */
package p;

interface T8193561 extends p.T8193561.I {

    interface I  extends T8193561 {
        default boolean m() {
            return false;
        }
    }

    default boolean m() {
        return false;
    }

}
