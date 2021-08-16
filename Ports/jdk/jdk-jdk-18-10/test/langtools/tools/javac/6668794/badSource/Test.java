/*
 * @test /nodynamiccopyright/
 * @bug 6668794 6668796
 * @summary javac puts localized text in raw diagnostics
 *      bad diagnostic "bad class file" given for source files
 * @compile/fail/ref=Test.out -XDrawDiagnostics Test.java
 */

class Test {
    p.A a;
}
