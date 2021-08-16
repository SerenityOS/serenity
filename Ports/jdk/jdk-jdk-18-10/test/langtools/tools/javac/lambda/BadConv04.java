/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that ill-formed SAM type generates right diagnostic when SAM converted
 * @compile/fail/ref=BadConv04.out -XDrawDiagnostics BadConv04.java
 */

class BadConv04 {

    interface I1 {
        int m();
    }

    interface I2 {
        long m();
    }

    interface SAM extends I1, I2 {}

    SAM s = ()-> { };
}
