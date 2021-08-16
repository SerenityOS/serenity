/*
 * @test /nodynamiccopyright/
 * @bug 5019609 8246774
 * @summary javac fails to reject local enums
 * @author gafter
 * @compile/fail/ref=LocalEnum.out -XDrawDiagnostics -source 15 LocalEnum.java
 * @compile LocalEnum.java
 */

public class LocalEnum {
    void f() {
        enum B {}
    }
}
