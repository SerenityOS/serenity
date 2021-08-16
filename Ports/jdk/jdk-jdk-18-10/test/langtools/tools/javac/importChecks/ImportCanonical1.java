/*
 * @test /nodynamiccopyright/
 * @bug 4462745
 * @summary compiler permits to import class given by its non-canonical name
 * @author gafter
 *
 * @compile/fail/ref=ImportCanonical1.out -XDrawDiagnostics  ImportCanonical1.java ImportCanonical2.java
 */

package p1;
class A1 { static class I {} }
class A2 extends A1 {}
