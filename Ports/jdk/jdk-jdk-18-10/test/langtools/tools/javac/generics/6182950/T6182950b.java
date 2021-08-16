/*
 * @test /nodynamiccopyright/
 * @bug     6182950
 * @summary methods clash algorithm should not depend on return type
 * @author  mcimadamore
 * @compile/fail/ref=T6182950b.out -XDrawDiagnostics T6182950b.java
 */
import java.util.List;

class T6182950b {
    static class A {
        int m(List<String> l) {return 0;}
    }
    static class B extends A {
        double m(List<Integer> l) {return 0;}
    }
}
