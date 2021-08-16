/**
 * @test    /nodynamiccopyright/
 * @bug     7169362 8001114
 * @author  sogoel
 * @summary ContainerAnnotation does not have FooContainer.class specified
 * @compile/fail/ref=MissingContainer.out -XDrawDiagnostics MissingContainer.java
 */

import java.lang.annotation.Repeatable;

@Repeatable
@interface Foo {}

@interface FooContainer {
    Foo[] value();
}

@Foo @Foo
public class MissingContainer {}
