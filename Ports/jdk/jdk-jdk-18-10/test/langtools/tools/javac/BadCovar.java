/*
 * @test /nodynamiccopyright/
 * @bug 5007379
 * @summary Compiler allows inheritance of multiple methods with unrelated return types
 * @author gafter
 *
 * @compile/fail/ref=BadCovar.out -XDrawDiagnostics  BadCovar.java
 */

package bad.covar;

import java.util.*;

interface A{
        List<? extends A> f();
}

interface B{
        List<? extends B> f();
}

abstract class C implements A, B {} // should give error
