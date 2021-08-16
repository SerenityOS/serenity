/*
 * @test /nodynamiccopyright/
 * @bug 8167000
 * @summary Refine handling of multiple maximally specific abstract methods
 * @compile/fail/ref=T8167000.out -XDrawDiagnostics -Werror -Xlint:unchecked T8167000.java
 */

import java.util.*;

class T8167000 {

    interface J {
        List<Number> getAll(String str);
    }

    interface K {
        Collection<Integer> getAll(String str);
    }

    interface L {
        List getAll(String str);
    }

    interface M {
        Collection getAll(String str);
    }


    static abstract class E implements J, K, L, M {
        void test() {
            List<String> l = getAll(""); //check that we get an unchecked warning here
        }
    }
}
