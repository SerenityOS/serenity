/*
 * @test /nodynamiccopyright/
 * @bug 4717193
 * @summary javac improperly allows null + 1
 * @author gafter
 * @compile/fail/ref=NullAppend2.out -XDrawDiagnostics  NullAppend2.java
 */

class NullAppend2 {{
    String s = null + 1;
}}
