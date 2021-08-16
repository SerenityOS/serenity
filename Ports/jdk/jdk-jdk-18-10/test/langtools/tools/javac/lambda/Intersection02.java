/*
 * @test /nodynamiccopyright/
 * @bug 8010101
 * @summary Intersection type cast issues redundant unchecked warning
 * @compile/fail/ref=Intersection02.out -Werror -Xlint:unchecked -XDrawDiagnostics Intersection02.java
 */
import java.io.Serializable;
import java.util.List;

class Intersection02 {

    interface P<X> { }

    void test(List<String> ls) {
        Object o1 = (List<String> & Serializable)ls;
        Object o2 = (List<String> & Serializable & P<String>)ls;
    }
}
