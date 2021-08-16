/*
 * @test /nodynamiccopyright/
 * @bug 8231827
 * @summary Verify behavior w.r.t. non-reifiable types in instanceof
 * @compile/fail/ref=ReifiableOld-old.out -source 15 -Xlint:-options -XDrawDiagnostics ReifiableOld.java
 * @compile/fail/ref=ReifiableOld.out -XDrawDiagnostics ReifiableOld.java
 */

public class ReifiableOld implements ReifiableOldI {
    private static boolean test(Object o, List<ReifiableOld> l1, List<String> l2) {
        return o instanceof ListImpl<ReifiableOld> &&
               l1 instanceof ListImpl<ReifiableOld> &&
               l2 instanceof ListImpl<ReifiableOld> &&
               l2 instanceof ListImpl<String> &&
               l1 instanceof Unrelated<ReifiableOld>;
    }

    public class List<T> {}
    public class ListImpl<T extends ReifiableOldI> extends List<T> {}
    public class Unrelated<T> {}
}

interface ReifiableOldI {}
