/*
 * @test /nodynamiccopyright/
 * @bug 8013222
 * @summary Javac issues spurious raw type warnings when lambda has implicit parameter types
 * @compile/fail/ref=NoWarnOnImplicitParams.out -Xlint:rawtypes -Werror -XDrawDiagnostics NoWarnOnImplicitParams.java
 */
import java.util.List;

class NoWarnOnImplicitParams {

    public void testRawMerge(List<String> ls) {
        R12 r12_1 = l->"Foo";
        R12 r12_2 = (List l)->"Foo";
    }

    interface R1 {
        Object m(List<String> ls);
    }

    @SuppressWarnings("rawtypes")
    interface R2 {
        String m(List l);
    }

    interface R12 extends R1, R2 {}
}
