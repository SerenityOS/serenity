import java.lang.annotation.*;
import java.util.List;

/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary test that compiler doesn't warn about annotated redundant casts
 * @author Mahmood Ali
 * @author Werner Dietl
 * @compile/ref=LintCast.out -Xlint:cast -XDrawDiagnostics LintCast.java
 */
class LintCast {
    void unparameterized() {
        String s = "m";
        String s1 = (String)s;
        String s2 = (@A String)s;
    }

    void parameterized() {
        List<String> l = null;
        List<String> l1 = (List<String>)l;
        List<String> l2 = (List<@A String>)l;
    }

    void array() {
        int @A [] a = null;
        int[] a1 = (int[])a;
        int[] a2 = (int @A [])a;
    }

    void sameAnnotations() {
        @A String annotated = null;
        String unannotated = null;

        // compiler ignore annotated casts even if redundant
        @A String anno1 = (@A String)annotated;

        // warn if redundant without an annotation
        String anno2 = (String)annotated;
        String unanno2 = (String)unannotated;
    }

    void more() {
        Object @A [] a = null;
        Object[] a1 = (Object[])a;
        Object[] a2 = (Object @A [])a;

        @A List<String> l3 = null;
        List<String> l4 = (List<String>)l3;
        List<String> l5 = (@A List<String>)l3;

        List<@A String> l6 = null;
        List<String> l7 = (List<String>)l6;
        List<String> l8 = (List<@A String>)l6;

        @A Object o = null;
        Object o1 = (Object)o;
        Object o2 = (@A Object)o;

        Outer. @A Inner oi = null;
        Outer.Inner oi1 = (Outer.Inner)oi;
        Outer.Inner oi2 = (Outer. @A Inner)oi;
    }

    class Outer { class Inner {} }
}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface A { }
