/*
 * @test /nodynamiccopyright/
 * @bug     6968793
 * @summary javac generates spourious diagnostics for ill-formed type-variable bounds
 * @author  mcimadamore
 * @compile/fail/ref=T6968793.out -XDrawDiagnostics T6968793.java
 */

class T6968793<X extends Number, Y extends X, Z extends Object & Comparable<Y>> {
    T6968793<Object, Object, Object> o;
}
