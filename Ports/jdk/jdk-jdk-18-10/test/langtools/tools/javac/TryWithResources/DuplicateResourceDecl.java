/*
 * @test  /nodynamiccopyright/
 * @bug 6911256 6964740 6965277
 * @author Maurizio Cimadamore
 * @summary Check that resource variable is not accessible from catch/finally clause
 * @compile/fail/ref=DuplicateResourceDecl.out -XDrawDiagnostics DuplicateResourceDecl.java
 */

class DuplicateResourceDecl {

    public static void main(String[] args) {
        try(MyResource c = new MyResource();MyResource c = new MyResource()) {
        //do something
        } catch (Exception e) { }
    }

    static class MyResource implements AutoCloseable {
        public void close() throws Exception {}
    }
}
