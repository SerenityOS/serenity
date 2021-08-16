/**
 * @test    /nodynamiccopyright/
 * @bug     7169362
 * @author  sogoel
 * @summary Wrong return type for value() in ContainerAnnotation
 * @compile/fail/ref=WrongReturnTypeForValue.out -XDrawDiagnostics WrongReturnTypeForValue.java
 */

import java.lang.annotation.Repeatable;

@Repeatable(FooContainer.class)
@interface Foo {
    int getNumbers();
}

@interface FooContainer{
    Foo value();     // wrong return type
}

@Foo @Foo
public class WrongReturnTypeForValue {}
