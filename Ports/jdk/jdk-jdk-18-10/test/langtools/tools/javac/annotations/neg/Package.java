/*
 * @test /nodynamiccopyright/
 * @bug 4901290
 * @summary Package annotations
 * @author gafter
 *
 * @compile/fail/ref=Package.out -XDrawDiagnostics  Package.java
 */

@java.lang.annotation.Documented
package foo.bar;
