/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that recovery of speculative types is not attempted if receiver is erroneous
 * @compile/fail/ref=BadRecovery.out -XDrawDiagnostics BadRecovery.java
 */
class BadRecovery {

    interface SAM1 {
        void m(Object o);
    }

    void m(SAM1 m) { };

    void test() {
        m((receiver, t) -> { receiver.someMemberOfReceiver(()->{ Object x = f; }); });
    }
}
