/*
 * @test /nodynamiccopyright/
 * @bug 8007464
 * @summary Add graph inference support
 *          more smoke tests for graph inference
 * @compile/fail/ref=TargetType57.out -XDrawDiagnostics TargetType57.java
 */
import java.util.*;
import java.util.function.*;

class TargetType57 {

    void test(List<Integer> list) {
        m(list, s -> s.intValue(), s -> s.nonExistentMethod());
    }

    <U, R, S_IN, S_OUT> R m(List<S_IN> list,
                        Function<S_IN, S_OUT> f1,
                        Function<S_OUT, R> f2) { return null; }
}
