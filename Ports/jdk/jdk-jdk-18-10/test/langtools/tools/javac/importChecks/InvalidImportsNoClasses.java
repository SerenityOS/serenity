/*
 * @test /nodynamiccopyright/
 * @bug 4041851 4312063
 * @summary Verify that nonexistent imports detected when no classes declared in compilation unit.
 * @author maddox
 *
 * @compile/fail/ref=InvalidImportsNoClasses.out -XDrawDiagnostics  InvalidImportsNoClasses.java
 */

import nonexistent.pack.cls;
