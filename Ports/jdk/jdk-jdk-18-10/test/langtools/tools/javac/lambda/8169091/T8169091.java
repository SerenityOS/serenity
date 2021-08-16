/*
 * @test
 * @bug 8169091
 * @summary Method reference T::methodName for generic type T does not compile any more
 * @compile T8169091.java
 */

import java.io.Serializable;
import java.util.Comparator;

interface T8169091 {
    static <T extends Comparable<? super T>> Comparator<T> comparator() {
        return (Comparator<T> & Serializable)T::compareTo;
    }
}
