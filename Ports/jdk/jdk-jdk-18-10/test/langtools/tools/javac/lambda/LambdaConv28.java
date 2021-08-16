/*
 * @test /nodynamiccopyright/
 * @bug 8181911
 * @summary Verify that the analyzer does not affect ordinary compilation.
 * @compile/ref=LambdaConv28.out -XDrawDiagnostics -XDfind=lambda LambdaConv28.java
 */

class LambdaConv28 {

    public void test(A a) {
        test(()-> {
            return new I() {
                public <T> void t() {
                }
            };
        });
        test(new A() {
            public I get() {
                return null;
            }
        });
    }

    public interface I {
        public <T> void t();
    }

    public interface A {
        public I get();
    }

}
