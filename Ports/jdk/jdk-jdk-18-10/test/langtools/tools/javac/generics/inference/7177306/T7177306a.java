/**
 * @test /nodynamiccopyright/
 * @bug 7177306
 * @summary Regression: unchecked method call does not erase return type
 * @compile/fail/ref=T7177306a.out -Werror -Xlint:unchecked -XDrawDiagnostics T7177306a.java
 */

import java.util.List;

class T7177306a<A> {

    public static void test(List l) {
        T7177306a<Object> to = m(l);
    }

    public static <E> T7177306a<String> m(List<E> le) {
        return null;
    }
}
