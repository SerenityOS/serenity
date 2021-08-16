/**
 * @test /nodynamiccopyright/
 * @summary Container annotation is not checked for semantic correctness
 * @bug 8001114
 *
 * @compile/fail/ref=RepeatingTargetNotAllowed.out -XDrawDiagnostics RepeatingTargetNotAllowed.java
 */

import java.lang.annotation.*;

@Repeatable(Foos.class)
@interface Foo {}

@Target(ElementType.ANNOTATION_TYPE)
@interface Foos {
    Foo[] value();
}

public class RepeatingTargetNotAllowed {
    @Foo @Foo int f = 0;
}
