/*
 * @test /nodynamiccopyright/
 * @summary Smoke test for repeating annotations
 * @compile/fail/ref=UseWrongRepeatable.out -XDrawDiagnostics  UseWrongRepeatable.java
 * @bug 7151010
 */

import java.lang.annotation.*;

@interface Foos {
    UseWrongRepeatable[] value();
}

@Repeatable(Target.class)
public @interface UseWrongRepeatable {}

@UseWrongRepeatable @UseWrongRepeatable
@interface Foo {}
