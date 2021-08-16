/*
 * @test /nodynamiccopyright/
 * @bug 8261006
 * @summary 'super' qualified method references cannot occur in a static context
 * @compile/fail/ref=MethodReferenceInConstructorInvocation.out -XDrawDiagnostics MethodReferenceInConstructorInvocation.java
 */

import java.util.function.Supplier;

public class MethodReferenceInConstructorInvocation {
    interface Bar {
        default String getString() {
            return "";
        }
    }

    static class Foo implements Bar {

        Foo() {
            this(Bar.super::getString);
        }
        Foo(Supplier<String> sString) {}

        Foo(int i) { this(Bar.super.getString()); }
        Foo(String s) {}
    }
}
