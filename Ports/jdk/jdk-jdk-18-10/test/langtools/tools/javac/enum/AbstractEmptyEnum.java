/*
 * @test /nodynamiccopyright/
 * @bug 5009601
 * @summary empty enum cannot be abstract
 * @author Joseph D. Darcy
 *
 * @compile/fail/ref=AbstractEmptyEnum.out -XDrawDiagnostics  AbstractEmptyEnum.java
 */

public enum AbstractEmptyEnum {
    ;
    abstract void m();
}
