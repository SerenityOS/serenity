/*
 * @test /nodynamiccopyright/
 * @summary Test subtyping for wildcards with the same type bound.
 *
 * @compile/fail/ref=AssignmentSameType.out -XDrawDiagnostics AssignmentSameType.java
 */

public class AssignmentSameType {

    public static void main(String[] args) {
        Ref<B> exact = null;
        Ref<? extends B> ebound = null;
        Ref<? super B> sbound = null;
        Ref<?> unbound = null;

;       exact = exact;          // <<pass>> <A> = <A>

        ebound = exact;         // <<pass>> <? extends A> = <A>
        ebound = ebound;        // <<pass>> <? extends A> = <? extends A>

        sbound = exact;         // <<pass>> <? super A> = <A>
        sbound = sbound;        // <<pass>> <? super A> = <? super A>

        unbound = exact;        // <<pass>> <?> = <A>
        unbound = ebound;       // <<pass>> <?> = <? extends A>
        unbound = sbound;       // <<pass>> <?> = <? super A>
        unbound = unbound;      // <<pass>> <?> = <?>

        exact = ebound;         // <<fail>> <A> = <? extends A>
        exact = sbound;         // <<fail>> <A> = <? super A>
        exact = unbound;        // <<fail>> <A> = <?>
        ebound = sbound;        // <<fail>> <? extends A> = <? super A>
        ebound = unbound;       // <<fail>> <? extends A> = <?>
        sbound = ebound;        // <<fail>> <? super A> = <? extends A>
        sbound = unbound;       // <<fail>> <? super A> = <?>
    }
}

class Ref<A> {}
class B {}
