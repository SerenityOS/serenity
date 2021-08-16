/**
 * @test    /nodynamiccopyright/
 * @bug     7169362
 * @author   sogoel
 * @summary Base anno is Inherited but Container anno is not
 * @compile/fail/ref=InheritedContainerAnno.out -XDrawDiagnostics InheritedContainerAnno.java
 */

import java.lang.annotation.Repeatable;
import java.lang.annotation.Inherited;

@Inherited
@Repeatable(FooContainer.class)
@interface Foo {}

@interface FooContainer{
    Foo[] value();
}

@Foo @Foo
public class InheritedContainerAnno {}

