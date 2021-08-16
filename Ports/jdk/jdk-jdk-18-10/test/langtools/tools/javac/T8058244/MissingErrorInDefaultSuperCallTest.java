/*
 * @test /nodynamiccopyright/
 * @bug 8058244
 * @summary missing error in qualified default super call
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @compile/fail/ref=MissingErrorInDefaultSuperCallTest.out -XDrawDiagnostics MissingErrorInDefaultSuperCallTest.java
 */

public class MissingErrorInDefaultSuperCallTest {
    interface I {
        default int f(){return 0;}
    }

    class J implements I {}

    class T extends J implements I {
        public int f() {
            return I.super.f();
        }
    }
}
