/*
 * @test /nodynamiccopyright/
 * @bug 4951670 7170058
 * @summary javac crash with improper overrider
 * @author gafter
 *
 * @compile/fail/ref=ErasureClashCrash.out -XDrawDiagnostics  ErasureClashCrash.java
 */

interface Compar<T> {
    int compareTo(T o);
}
abstract class ErasureClashCrash implements Compar<ErasureClashCrash> {
    public int compareTo(Object o) {
        return 1;
    }
}
