/*
 * @test /nodynamiccopyright/
 * @bug 4948130
 * @summary casting conversion checks changed for covariant returns
 * @author gafter
 *
 * @compile/fail/ref=RefEqual.out -XDrawDiagnostics  RefEqual.java
 */

class RefEqual {
    {
        Class c = null;
        if (String.class != Integer.class) ;
    }
}
