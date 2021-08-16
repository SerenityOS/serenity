/*
 * @test  /nodynamiccopyright/
 * @bug 4279339 6969184
 * @summary Verify that an anonymous class can contain a static method only if source >= 16
 * @author maddox
 *
 * @compile/fail/ref=AnonStaticMember_2.out -source 15 -XDrawDiagnostics AnonStaticMember_2.java
 * @compile AnonStaticMember_2.java
 */

class AnonStaticMember_2 {
    Object x = new Object() {
        static void test() {}
    };
}
