/**
 * @test /nodynamiccopyright/
 * @bug 8004099
 * @summary Bad compiler diagnostic generated when poly expression is passed to non-existent method
 * @compile/fail/ref=BadMethodCall2.out -XDrawDiagnostics BadMethodCall2.java
 */
class BadMethodCall2 {
     void test(Object rec) {
         rec.nonExistent(System.out::println);
         rec.nonExistent(()->{});
         rec.nonExistent(true ? "1" : "2");
     }
}
