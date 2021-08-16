/*
 * @test /nodynamiccopyright/
 * @bug 6511613
 * @summary javac unexpectedly doesn't fail in some cases if an annotation processor specified
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor DummyProcessor
 * @compile/fail/ref=clss41701.out -XDrawDiagnostics clss41701.java
 * @compile/fail/ref=clss41701.out -XDrawDiagnostics -processor DummyProcessor clss41701.java
 */

import java.io.PrintStream;

interface clss41701i {
    void run();
}

class clss41701a<A extends clss41701i,
                 B extends clss41701i,
                 C extends A&B> {
}
