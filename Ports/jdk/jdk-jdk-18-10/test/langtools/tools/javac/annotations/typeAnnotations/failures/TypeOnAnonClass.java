/*
 * @test /nodynamiccopyright/
 * @bug 8023682
 * @summary Cannot annotate an anonymous class with a target type of TYPE
 * @compile/fail/ref=TypeOnAnonClass.out -XDrawDiagnostics TypeOnAnonClass.java
 */
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface X {}
interface Foo {}
class TypeOnAnonClass { void m() { new @X Foo() {}; } }
