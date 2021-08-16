/**
 * @test    /nodynamiccopyright/
 * @bug     7169362
 * @author  sogoel
 * @summary Missing default case for other method and return type is base annotation
 * @compile/fail/ref=MissingDefaultCase2.out -XDrawDiagnostics MissingDefaultCase2.java
 */

import java.lang.annotation.Repeatable;

@Repeatable(FooContainer.class)
@interface Foo {}

@interface FooContainer {
    Foo[] value();
    Foo other();  // missing default clause and return type is an annotation
}

@Foo @Foo
public class MissingDefaultCase2 {}
