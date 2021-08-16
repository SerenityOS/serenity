/*
 * @test /nodynamiccopyright/
 * @bug 4992316
 * @summary compiler allows new array of array of type parameter
 * @author gafter
 *
 * @compile/fail/ref=UncheckedArray.out -XDrawDiagnostics  UncheckedArray.java
 */

package unchecked.array;

class J<T> {
    {
        Object o = new T[3][];
    }
}
