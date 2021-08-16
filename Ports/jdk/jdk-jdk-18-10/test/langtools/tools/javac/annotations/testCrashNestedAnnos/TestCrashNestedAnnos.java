/* @test /nodynamiccopyright/
 * @bug 8027375
 * @summary Test that javac doesn't assert/crash when there are what looks to
 *          be annotations nested inside erroneous annotations.
 * @compile/fail/ref=TestCrashNestedAnnos.out -XDrawDiagnostics TestCrashNestedAnnos.java
 */
public class TestCrashNestedAnnos {
    // A and B are not annotation types
    @A(@A1()) int foo() {}
    @B(@B1()) int bar() {}
}

class B {}
class B1 {}
