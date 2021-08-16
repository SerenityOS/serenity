/*
 * @test /nodynamiccopyright/
 * @bug 8013485
 * @summary Annotations that gets a clinit can't be verified for correct elements in a second compilation unit
 * @compile/fail/ref=AnnoWithClinitFail.out -XDrawDiagnostics AnnoWithClinitFail.java
 */

public @interface AnnoWithClinitFail {
    Foo f = new Foo();

    String foo();
    String bar() default "bar";

    @AnnoWithClinitFail
    static class C {} // this is in the same CU so there wont be a
                      // <clinit> when the this anno instance is checked

    class Foo {}
}

@AnnoWithClinitFail
class TestAnnoWithClinitFail { }
