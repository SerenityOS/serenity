/**
 * @test /nodynamiccopyright/
 * @bug 7196531
 * @compile/fail/ref=DuplicateErrors.out  -XDrawDiagnostics DuplicateErrors.java
 */


@interface Foo {}

@Foo
@Foo
@Foo
public class DuplicateErrors {
}
