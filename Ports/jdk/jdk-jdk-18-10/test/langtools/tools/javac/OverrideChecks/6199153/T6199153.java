/**
 * @test /nodynamiccopyright/
 * @bug 6199153
 * @summary Generic throws and overriding
 * @author  mcimadamore
 * @compile/fail/ref=T6199153.out -Xlint -Werror -XDrawDiagnostics T6199153.java
 */

import java.io.IOException;

class T6199153 {

    static class A {
        public <T extends IOException> void m() throws T {}
    }

    static class B extends A {
        public void m() throws IOException {}
    }
}

