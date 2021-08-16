/*
 * @test /nodynamiccopyright/
 * @bug 8173456
 * @summary com.sun.tools.javac.util.Assert.error during code compilation
 * @compile/fail/ref=MethodReferenceNoThisTest.out -XDrawDiagnostics MethodReferenceNoThisTest.java
 */

import java.util.function.Function;

abstract class MethodReferenceNoThisTest_Base {
  protected MethodReferenceNoThisTest_Base(Function<MethodReferenceNoThisTest_Base, MethodReferenceNoThisTest_AV> x) {}
}

abstract class MethodReferenceNoThisTest_AV {
    MethodReferenceNoThisTest_AV(MethodReferenceNoThisTest_Base b) {
    }
}

public class MethodReferenceNoThisTest extends MethodReferenceNoThisTest_Base {

    public MethodReferenceNoThisTest() {
        super(V::new);
    }

    private class V extends MethodReferenceNoThisTest_AV {

        V(MethodReferenceNoThisTest_Base b) {
            super(b);
        }
    }
}