/*
 * @test /nodynamiccopyright/
 * @bug 8231827
 * @summary Verify behavior w.r.t. non-reifiable types and type test patterns in instanceof
 * @compile/fail/ref=Reifiable.out -XDrawDiagnostics Reifiable.java
 */

public class Reifiable implements ReifiableI {
    private static boolean test(Object o, List<Reifiable> l1, List<String> l2) {
        return o instanceof ListImpl<Reifiable> li1 &&
               l1 instanceof ListImpl<Reifiable> li2 &&
               l2 instanceof ListImpl<Reifiable> li3 &&
               l2 instanceof ListImpl<String> li4 &&
               l1 instanceof Unrelated<Reifiable> li5;
    }

    public class List<T> {}
    public class ListImpl<T extends ReifiableI> extends List<T> {}
    public class Unrelated<T> {}
}

interface ReifiableI {}
