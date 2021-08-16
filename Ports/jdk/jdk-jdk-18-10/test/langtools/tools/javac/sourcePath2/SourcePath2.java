/*
 * @test /nodynamiccopyright/
 * @bug 4648973
 * @summary compiler does not emit code for second class in source file
 * @author gafter
 *
 * @compile/fail/ref=SourcePath2.out -XDrawDiagnostics  SourcePath2.java
 */

import p.SourcePath2A;

public class SourcePath2 {}
