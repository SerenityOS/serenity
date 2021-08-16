/*
 * @test /nodynamiccopyright/
 * @bug 4402884
 * @summary javac improperly extends superclass's scope to implements clause
 * @author gafter
 *
 * @clean I
 * @compile/fail/ref=ExtendsScope.out -XDrawDiagnostics ExtendsScope.java
 */

class P {
    interface I {}
}

class T extends P implements I { // error: no I in scope
}
