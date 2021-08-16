/**
 * @test    /nodynamiccopyright/
 * @bug     7169362 8001114
 * @author  sogoel
 * @summary Default case not specified for other methods in container annotation
 * @compile/fail/ref=MissingDefaultCase1.out -XDrawDiagnostics MissingDefaultCase1.java
 */

import java.lang.annotation.Repeatable;

@Repeatable(FooContainer.class)
@interface Foo {}

@interface FooContainer {
    Foo[] value();
    String other();  // missing default clause
}

@Foo @Foo
public class MissingDefaultCase1 {}
