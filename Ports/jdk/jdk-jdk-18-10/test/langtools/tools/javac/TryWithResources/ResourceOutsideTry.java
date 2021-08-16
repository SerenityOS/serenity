/*
 * @test  /nodynamiccopyright/
 * @bug 6911256 6964740 6965277
 * @author Maurizio Cimadamore
 * @summary Check that resource variable is not accessible from catch/finally clause
 * @compile/fail/ref=ResourceOutsideTry.out -XDrawDiagnostics ResourceOutsideTry.java
 */

class ResourceOutsideTry {
    void test() {
        try(MyResource c = new MyResource()) {
        //do something
        } catch (Exception e) {
            c.test();
        } finally {
            c.test();
        }
    }
    static class MyResource implements AutoCloseable {
        public void close() throws Exception {}
        void test() {}
    }
}
