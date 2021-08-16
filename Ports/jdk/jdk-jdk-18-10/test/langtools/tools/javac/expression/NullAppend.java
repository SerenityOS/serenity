/*
 * @test  /nodynamiccopyright/
 * @bug 4620794
 * @summary compiler allows null + null
 * @author gafter
 *
 * @compile/fail/ref=NullAppend.out -XDrawDiagnostics  NullAppend.java
 */

class NullAppend {{
    String s = null + null;
}}
