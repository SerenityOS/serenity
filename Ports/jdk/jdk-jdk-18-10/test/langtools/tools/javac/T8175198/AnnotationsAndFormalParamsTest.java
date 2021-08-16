/*
 * @test /nodynamiccopyright/
 * @bug 8175198
 * @summary Javac incorrectly allows receiver parameters in annotation methods
 * @compile/fail/ref=AnnotationsAndFormalParamsTest.out -XDrawDiagnostics -Werror -Xlint:unchecked AnnotationsAndFormalParamsTest.java
 */

@interface AnnotationsAndFormalParamsTest {
    int value(int i);
    int foo(AnnotationsAndFormalParamsTest this);
}
