/**
 * @test    /nodynamiccopyright/
 * @bug     7169362
 * @author  sogoel
 * @summary Base annotation specify itself as ContainerAnnotation
 * @compile/fail/ref=BaseAnnoAsContainerAnno.out -XDrawDiagnostics BaseAnnoAsContainerAnno.java
 */

import java.lang.annotation.Repeatable;

@Repeatable(Foo.class)
@interface Foo {
    Foo[] value() default {};
}

@Foo @Foo
public class BaseAnnoAsContainerAnno {}

