/**
 * @test    /nodynamiccopyright/
 * @bug     7169362
 * @author   sogoel
 * @summary Missing value() method in ContainerAnnotation
 * @compile/fail/ref=MissingValueMethod.out -XDrawDiagnostics MissingValueMethod.java
 */

import java.lang.annotation.Repeatable;

@Repeatable(FooContainer.class)
@interface Foo {}

@interface FooContainer{
    Foo[] values();  // wrong method name
}

@Foo @Foo
public class MissingValueMethod {}

