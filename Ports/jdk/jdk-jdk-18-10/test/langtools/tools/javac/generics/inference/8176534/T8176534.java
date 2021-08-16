/*
 * @test /nodynamiccopyright/
 * @bug 8176534
 * @summary Missing check against target-type during applicability inference
 * @compile/fail/ref=T8176534.out -Werror -Xlint:unchecked -XDrawDiagnostics T8176534.java
 */

import java.util.*;

abstract class T8176534 {
    List<String> f(Enumeration e) {
        return newArrayList(forEnumeration(e));
    }

    abstract <T> Iterator<T> forEnumeration(Enumeration<T> e);
    abstract <E> ArrayList<E> newArrayList(Iterator<? extends E> xs);
    abstract <E> ArrayList<E> newArrayList(Iterable<? extends E> xs);
}
