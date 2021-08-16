/**
 * @test  /nodynamiccopyright/
 * @bug 4986256
 * @compile/ref=Unchecked.noLint.out                         -XDrawDiagnostics Unchecked.java
 * @compile/ref=Unchecked.lintUnchecked.out -Xlint:unchecked -XDrawDiagnostics Unchecked.java
 * @compile/ref=Unchecked.lintAll.out       -Xlint:all,-path -XDrawDiagnostics Unchecked.java
 */

import java.util.ArrayList;
import java.util.List;

// control: this class should generate warnings
class Unchecked
{
    void m() {
        List l = new ArrayList<String>();
        l.add("abc");
    }
}

// tests: the warnings that would otherwise be generated should all be suppressed
@SuppressWarnings("unchecked")
class Unchecked2
{
    void m() {
        List l = new ArrayList<String>();
        l.add("abc");
    }
}

class Unchecked3
{
    @SuppressWarnings("unchecked")
    void m() {
        List l = new ArrayList<String>();
        l.add("abc");
    }
}

class Unchecked4
{
    void m() {
        @SuppressWarnings("unchecked")
            class Inner {
                void m() {
                    List l = new ArrayList<String>();
                    l.add("abc");
                }
            }
    }
}

// this class should produce warnings because @SuppressWarnings should not be inherited
class Unchecked5 extends Unchecked2
{
    void m() {
        List l = new ArrayList<String>();
        l.add("abc");
    }
}
