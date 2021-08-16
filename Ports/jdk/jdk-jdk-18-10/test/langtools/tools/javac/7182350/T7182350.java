/**
 * @test  /nodynamiccopyright/
 * @bug 7182350
 * @summary verify correct output of -Xlint:unchecked on methods with unchecked conversations in parameters
 * @compile/ref=T7182350.out -XDrawDiagnostics -Xlint:unchecked T7182350.java
 */

import java.util.*;

class T7182350 {
    public static void quicksort(Vector vector, Comparator compare) {
        Collections.sort(vector,compare);
    }
}
