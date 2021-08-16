/*
 * @test /nodynamiccopyright/
 * @bug     6182950
 * @summary methods clash algorithm should not depend on return type
 * @author  mcimadamore
 * @compile/fail/ref=T6182950a.out -XDrawDiagnostics T6182950a.java
 */
import java.util.List;

class T6182950a {
    int m(List<String> l) {return 0;}
    double m(List<Integer> l) {return 0;}
}
