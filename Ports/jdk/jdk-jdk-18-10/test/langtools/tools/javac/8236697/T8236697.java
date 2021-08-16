/*
 * @test /nodynamiccopyright/
 * @bug 8236697
 * @summary Stack overflow with cyclic hierarchy in class file
 * @build Cyclic
 * @compile/fail/ref=T8236697.out -XDrawDiagnostics T8236697.java
 */
interface T8236697 extends Iterable {

    public Cyclic iterator();

}
