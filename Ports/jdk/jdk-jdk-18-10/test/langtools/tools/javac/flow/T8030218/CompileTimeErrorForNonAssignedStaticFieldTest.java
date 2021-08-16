/*
 * @test /nodynamiccopyright/
 * @bug 8030218
 * @summary javac, compile time error isn't shown when final static field is not assigned, follow-up
 * @compile/fail/ref=CompileTimeErrorForNonAssignedStaticFieldTest.out -XDrawDiagnostics CompileTimeErrorForNonAssignedStaticFieldTest.java
 */

public class CompileTimeErrorForNonAssignedStaticFieldTest {
    private final static int i;

    public CompileTimeErrorForNonAssignedStaticFieldTest()
            throws InstantiationException {
        throw new InstantiationException("Can't instantiate");
    }

    static class Inner {
        private final int j;
        public Inner(int x)
                throws InstantiationException {
            if (x == 0) {
                throw new InstantiationException("Can't instantiate");
            } else {
                j = 1;
            }
            System.out.println(j);
        }
    }

}
