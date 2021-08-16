/*
 * @test  /nodynamiccopyright/
 * @bug 6911256 6964740 7013420
 * @author Joseph D. Darcy
 * @summary Verify invalid TWR block is not accepted.
 * @compile/fail/ref=TwrOnNonResource.out -XDrawDiagnostics TwrOnNonResource.java
 */

class TwrOnNonResource {
    public static void main(String... args) {
        try(TwrOnNonResource aonr = new TwrOnNonResource()) {
            System.out.println(aonr.toString());
        }
        try(TwrOnNonResource aonr = new TwrOnNonResource()) {
            System.out.println(aonr.toString());
        } finally {;}
        try(TwrOnNonResource aonr = new TwrOnNonResource()) {
            System.out.println(aonr.toString());
        } catch (Exception e) {;}
    }

    /*
     * A close method, but the class is <em>not</em> Closeable or
     * AutoCloseable.
     */
    public void close() {
        throw new AssertionError("I'm not Closable!");
    }
}
