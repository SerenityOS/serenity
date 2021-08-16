/*
 * @test  /nodynamiccopyright/
 * @bug 4479164
 * @summary Throws clauses incompatible with Object methods allowed in interfaces
 * @author gafter
 *
 * @compile/fail/ref=InterfaceObjectIncompatibility.out -XDrawDiagnostics  InterfaceObjectIncompatibility.java
 */

interface InterfaceObjectIncompatibility {

    String toString() throws java.io.IOException;
    int hashCode() throws Exception;

}
