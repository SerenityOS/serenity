/**
 * @test    /nodynamiccopyright/
 * @bug     7169362
 * @author  sogoel
 * @summary Foo is not a repeatable annotation but used as one.
 * @compile/fail/ref=NoRepeatableAnno.out -XDrawDiagnostics NoRepeatableAnno.java
 */

@interface Foo {}

@Foo @Foo
public class NoRepeatableAnno {}
