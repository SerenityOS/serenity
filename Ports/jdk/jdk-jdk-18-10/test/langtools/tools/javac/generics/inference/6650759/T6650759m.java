/*
 * @test    /nodynamiccopyright/
 * @bug     6650759 8078024
 * @summary Inference of formal type parameter (unused in formal parameters) is not performed
 * @compile/fail/ref=T6650759m.out T6650759m.java -XDrawDiagnostics
 */

import java.util.*;

class T6650759m {
    <Z> List<? super Z> m(List<? extends List<? super Z>> ls) {
        return ls.get(0);
    }

    void test() {
        ArrayList<ArrayList<Integer>> lli = new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> li = new ArrayList<Integer>();
        li.add(2);
        lli.add(li);
        List<? super String> ls = m(lli); //here
        ls.add("crash");
        Integer i = li.get(1);
    }
}
