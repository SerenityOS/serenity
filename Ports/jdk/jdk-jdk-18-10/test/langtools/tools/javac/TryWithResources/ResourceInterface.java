/*
 * @test /nodynamiccopyright/
 * @bug 6970833
 * @author Maurizio Cimadamore
 * @summary Try-with-resource implementation throws an NPE during Flow analysis
 * @compile/fail/ref=ResourceInterface.out -XDrawDiagnostics ResourceInterface.java
 */

class ResourceInterface {
    public void test1() {
        try(Resource1 r1 = null) { }
    }

    public void test2() {
        try(Resource2 r2 = null) { }
    }

    static class E1 extends Exception {}

    static class E2 extends Exception {}


    interface C1 extends AutoCloseable {
       void close() throws E1;
    }

    interface C2 extends AutoCloseable {
       void close() throws E2;
    }

    interface C3 extends AutoCloseable {
       void close() throws E2, E1;
    }

    static interface Resource1 extends C1, C2 {}

    static interface Resource2 extends C1, C3 {}
}
