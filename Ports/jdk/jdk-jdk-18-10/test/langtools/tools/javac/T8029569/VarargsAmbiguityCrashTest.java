/*
 * @test /nodynamiccopyright/
 * @bug 8029569 8037379
 * @summary internal javac cast exception when resolving varargs ambiguity
 * fix for JDK-8029569 doesn't cover all possible cases
 * @compile/fail/ref=VarargsAmbiguityCrashTest.out -XDrawDiagnostics VarargsAmbiguityCrashTest.java
 */

public class VarargsAmbiguityCrashTest {
    void m1() {
        m2(null, new Exception());
    }

    void m2(Long l) {}

    void m2(Exception... exception) {}

    void m2(Long l, Exception... exception) {}
}
