/*
 * @test /nodynamiccopyright/
 * @bug 4497044
 * @summary java.lang.StackOverflowError for cyclic inheritance
 *
 * @compile ClassCycle3a.java
 * @compile/fail/ref=ClassCycle3a.out -XDrawDiagnostics  ClassCycle3b.java
 */

interface ClassCycle3b {}
class ClassCycle3a implements ClassCycle3b {}
