/*
 * @test /nodynamiccopyright/
 * @bug 6794959
 * @summary add new switch -XDexpectKeys=key,key,...
 * @compile T6794959.java
 * @compile/fail/ref=T6794959a.out -XDrawDiagnostics  -XDfailcomplete=java.lang.String T6794959.java
 * @compile -XDfailcomplete=java.lang.String -XDexpectKeys=compiler.err.cant.resolve.location T6794959.java
 * @compile/fail/ref=T6794959b.out -XDrawDiagnostics -XDexpectKeys=compiler.err.cant.resolve.location T6794959.java
 */

class T6794959 {
    String s;
}
