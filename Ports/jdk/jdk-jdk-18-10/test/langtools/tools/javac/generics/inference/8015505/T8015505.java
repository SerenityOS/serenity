/**
 * @test /nodynamiccopyright/
 * @bug 8015505
 * @summary Spurious inference error when return type of generic method requires unchecked conversion to target
 * @compile/fail/ref=T8015505.out -Xlint:-options -source 7 -XDrawDiagnostics T8015505.java
 * @compile T8015505.java
 */

import java.util.List;

class T8015505 {

    <Z> List m() { return null; }

    void test() {
        List<?> l = m();
    }
}
