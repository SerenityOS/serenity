/*
 * @test /nodynamiccopyright/
 * @bug 8068626
 * @summary Add javac lint warning when the Deprecated annotation is used where it is a no-op
 *
 * @compile/fail/ref=DeprecatedAnnotationTest.out -Werror -Xlint:deprecation -XDrawDiagnostics package-info.java
 */

@Deprecated
package p;

class DeprecatedAnnotationTest implements AutoCloseable {

    void foo(@Deprecated int p) {

        @Deprecated int l;

        try (@Deprecated DeprecatedAnnotationTest r = new DeprecatedAnnotationTest()) {
            // ...
        } catch (@Deprecated Exception e) {

        }
    }

    @Override
    public void close() throws Exception {
        @SuppressWarnings("deprecation")  // verify that we are able to suppress.
        @Deprecated int x;
    }
}
