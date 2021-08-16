/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   This test is for identifying a non-SAM type: Having overloaded methods due to inheritance
 * @compile/fail/ref=NonSAM3.out -XDrawDiagnostics NonSAM3.java Helper.java
 */

import java.util.Collection;
import java.util.List;

public class NonSAM3 {
    void method() {
        //all of the following will have compile error: "the target type of a lambda conversion has multiple non-overriding abstract methods"
        FooBar fb = (Number n) -> 100;
        FooBar fb2 = (Integer i) -> 100;
        DE de = (List<Integer> list) -> 100;
        DE de2 = (List<?> list) -> 100;
        DE de3 = (List list) -> 100;
        DE de4 = (Collection<Integer> collection) -> 100;
        DE de5 = (Collection<?> collection) -> 100;
        DE de6 = (Collection collection) -> 100;
    }
}
