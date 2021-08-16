/*
 * @test /nodynamiccopyright/
 * @bug 8054964
 * @summary Invalid package annotations
 * @author sogoel
 *
 * @compile/fail/ref=InvalidPackageAnno.out -XDrawDiagnostics  bar/package-info.java
 */

package bar;

