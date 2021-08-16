/*
 * @test /nodynamiccopyright/
 * @bug 5073079
 * @summary Allow unchecked override of generified methods in
 * parameterless classes
 * @author Peter von der Ah\u00e9
 *
 * @compile Warn1.java
 * @compile/fail/ref=Warn1.out -XDrawDiagnostics -Xlint:unchecked -Werror Warn1.java
 */

interface Attribute<T> { }

interface AttributeSet1 {
    <T> Attribute<T> get(Class<T> category);
}

class AttributeSet1Impl implements AttributeSet1 {
    public Attribute get(Class category) { return null; }
}
