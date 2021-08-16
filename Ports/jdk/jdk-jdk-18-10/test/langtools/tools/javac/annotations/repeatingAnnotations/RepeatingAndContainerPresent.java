/*
 * @test /nodynamiccopyright/
 * @summary Smoke test for repeating annotations
 * @compile/fail/ref=RepeatingAndContainerPresent.out -XDrawDiagnostics  RepeatingAndContainerPresent.java
 * @bug 7151010
 */

import java.lang.annotation.*;

@Repeatable(Foos.class)
@interface Foo {}

@interface Foos {
    Foo[] value();
}


@Foo
@Foo
@Foos({})
public class RepeatingAndContainerPresent {}
