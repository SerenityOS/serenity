/*
 * @test /nodynamiccopyright/
 * @bug 4500240
 * @summary javac throws StackOverflowError for recursive inheritance
 *
 * @compile ClassCycle1a.java
 * @compile/fail/ref=ClassCycle1a.out -XDrawDiagnostics  ClassCycle1b.java
 */

interface ClassCycle1b {}
interface ClassCycle1a extends ClassCycle1b {}
