/*
 * @test /nodynamiccopyright/
 * @bug     6400189
 * @summary raw types and inference
 * @author  mcimadamore
 * @compile/fail/ref=T6400189a.out T6400189a.java -Xlint:unchecked -XDrawDiagnostics
 */

import java.lang.reflect.Constructor;
import java.lang.annotation.Documented;

class T6400189a {
    Constructor c = null;
    Documented d = c.getAnnotation(Documented.class);
}
