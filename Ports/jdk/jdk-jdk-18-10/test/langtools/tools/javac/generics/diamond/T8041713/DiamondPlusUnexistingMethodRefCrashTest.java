/*
 * @test /nodynamiccopyright/
 * @bug 8041713
 * @summary  Type inference of non-existent method references crashes the compiler
 * @compile/fail/ref=DiamondPlusUnexistingMethodRefCrashTest.out -XDrawDiagnostics DiamondPlusUnexistingMethodRefCrashTest.java
 */

public class DiamondPlusUnexistingMethodRefCrashTest<T> {
    DiamondPlusUnexistingMethodRefCrashTest<String> m =
        new DiamondPlusUnexistingMethodRefCrashTest<>(DiamondPlusUnexistingMethodRefCrashTest::doNotExists);
}
