/*
 * @test /nodynamiccopyright/
 * @bug 4668238 4721069
 * @summary compiler must reject interface "overriding" final Object meth.
 * @compile/fail/ref=InterfaceOverrideFinal.out -XDrawDiagnostics  InterfaceOverrideFinal.java
 */
public interface InterfaceOverrideFinal {
    void notify();
}
