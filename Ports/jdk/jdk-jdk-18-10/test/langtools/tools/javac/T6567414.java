/*
 * @test /nodynamiccopyright/
 * @bug 6567414
 * @summary javac compiler reports no source file or line on enum constant declaration error
 * @compile/fail/ref=T6567414.out -XDrawDiagnostics T6567414.java
 */
enum Test {
  FOO;
  Test() throws Exception {}
}

