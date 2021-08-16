/*
 * @test /nodynamiccopyright/
 * @bug 4916607
 * @summary an unbounded (bivariant) wildcard doesn't allow reading
 * @author gafter
 *
 * @compile/fail/ref=Unbounded.out -XDrawDiagnostics Unbounded.java
 */

import java.util.Stack;
class Bug {
    void f() {
        Stack<?> stack = null;
        String o = stack.pop();
    }
}
