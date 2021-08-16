/**
 * @test /nodynamiccopyright/
 * @bug 7086586
 * @summary Inference producing null type argument
 * @compile/fail/ref=T7086586.out -XDrawDiagnostics T7086586.java
 */
import java.util.List;

class T7086586 {

    <T> List<T> m(List<? super T> dummy) { return null; }

    void test(List<?> l) {
        String s = m(l).get(0);
        Number n = m(l).get(0);
        Exception e = m(l).get(0);
        m(l).nonExistentMethod();
    }
}
