/*
 * @test /nodynamiccopyright/
 * @bug 4607420
 * @summary A bug in the original JSR14 generics specification
 *          created a loophole in the type system.
 *
 * @compile/fail/ref=Nonlinear.out -XDrawDiagnostics  Nonlinear.java
 */


public class Nonlinear {

    // This is an example of lack of type safety for
    // the version of javac from jsr14_adding_generics-1_0-ea

    // It is a variant of the "classic" problem with polymorphic
    // references in SML, which resulted in the usual array of
    // fixes: notably value polymorphism.

    // This code compiles, but produces a ClassCastException
    // when executed, even though there are no explicit casts in
    // the program.

    public static void main (String [] args) {
        Integer x = Integer.valueOf(5);
        String y = castit (x);
        System.out.println (y);
    }

    static <A,B> A castit (B x) {
        // This method casts any type to any other type.
        // Oh dear.  This shouldn't type check, but does
        // because build () returns a type Ref<*>
        // which is a subtype of RWRef<A,B>.
        final RWRef<A,B> r = build ();
        r.set (x);
        return r.get ();
    }

    static <A> Ref<A> build () {
        return new Ref<A> ();
    }

    // Another way of doing this is a variant of the crackit
    // example discussed in the draft specification.
    //
    // The original duplicate was:
    //
    // static <A> Pair <A,A> duplicate (A x) {
    //     return new Pair<A,A> (x,x);
    // }
    //
    // which breaks the requirement that a type variable
    // instantiated by * only occurs once in the result type.
    //
    // However, we can achieve the same result with a different
    // type for duplicate, which uses its type variables linearly
    // in the result:

    static <A,B extends Ref<A>> Pair<Ref<A>,B> duplicate (B x) {
        return new Pair<Ref<A>,B> (x,x);
    }

    // the cheat here is that A and B are used linearly in the result
    // type, but not in the polymorphic bounds.

    // We can use that to give an alternative implementation of
    // castit.

    static <A,B> A castit2 (B x) {
        Pair <Ref<A>, Ref<B>> p = duplicate (build ());
        p.snd.set (x);
        return p.fst.get ();
    }


}

interface RWRef<A,B> {

    public A get ();
    public void set (B x);

}

class Ref<A> implements RWRef <A,A> {

    A contents;

    public void set (A x) { contents = x; }
    public A get () { return contents; }

}

class Pair<A,B> {

    final A fst;
    final B snd;

    Pair (A fst, B snd) { this.fst = fst; this.snd = snd; }

}
