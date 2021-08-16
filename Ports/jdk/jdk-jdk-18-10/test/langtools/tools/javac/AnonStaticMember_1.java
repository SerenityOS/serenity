/*
 * @test /nodynamiccopyright/
 * @bug 4279339
 * @summary Verify that an anonymous class can contain a static field only if source >= 16
 * @author maddox
 *
 * @compile/fail/ref=AnonStaticMember_1.out -source 15 -XDrawDiagnostics AnonStaticMember_1.java
 * @compile AnonStaticMember_1.java
 */

class AnonStaticMember_1 {
    Object x = new Object() {
        static int y;
    };
}
