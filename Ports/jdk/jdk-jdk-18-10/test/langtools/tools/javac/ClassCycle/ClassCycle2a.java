/*
 * @test /nodynamiccopyright/
 * @bug 4500240
 * @summary javac throws StackOverflowError for recursive inheritance
 *
 * @compile ClassCycle2a.java
 * @compile/fail/ref=ClassCycle2a.out -XDrawDiagnostics  ClassCycle2b.java
 */

class ClassCycle2b {}
class ClassCycle2a extends ClassCycle2b {}
