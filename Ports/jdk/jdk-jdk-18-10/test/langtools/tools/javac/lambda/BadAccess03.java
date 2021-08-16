/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check lambda cannot assign non-effectively final locals
 * @compile/fail/ref=BadAccess03.out -XDrawDiagnostics BadAccess03.java
 */

class BadAccess03 {
    void test() {
        int k = 0;
        int n = 2; //effectively final variable
        Runnable r = ()-> { k = n; }; //error
    }
}
