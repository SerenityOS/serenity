/*
 * @test /nodynamiccopyright/
 * @bug     6804733
 * @summary javac generates spourious diagnostics for ill-formed type-variable bounds
 * @author  mcimadamore
 * @compile/fail/ref=T6804733.out -XDrawDiagnostics T6804733.java
 */

import java.util.ArrayList;
class T6804733<S> extends ArrayList<S> {
    <T extends S & S> void m() {}
}
