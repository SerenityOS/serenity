/*
 * @test /nodynamiccopyright/
 * @bug 8243047
 * @summary javac should not crash while processing exits in class initializers in Flow
 * @compile/fail/ref=ClassBlockExitsErrors.out -XDrawDiagnostics -XDshould-stop.at=FLOW ClassBlockExitsErrors.java
 */

public class ClassBlockExitsErrors {
    class I1 {
        {
            return;
        }
        void test() {}
    }

    class I2 {
        {
            break;
        }
        void test() {}
    }

    class I3 {
        {
            continue;
        }
        void test() {}
    }
}