/**
 * @test    /nodynamiccopyright/
 * @bug     7169362
 * @author  sogoel
 * @summary Base anno is Documented but Container anno is not
 * @compile/fail/ref=DocumentedContainerAnno.out -XDrawDiagnostics DocumentedContainerAnno.java
 */

import java.lang.annotation.Repeatable;
import java.lang.annotation.Documented;

@Documented
@Repeatable(FooContainer.class)
@interface Foo {}

@interface FooContainer{
    Foo[] value();
}

@Foo @Foo
public class DocumentedContainerAnno {}
