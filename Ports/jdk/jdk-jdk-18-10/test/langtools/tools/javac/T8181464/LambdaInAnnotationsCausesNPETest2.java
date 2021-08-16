/* @test /nodynamiccopyright/
 * @bug 8181464
 * @summary Invalid lambda in annotation causes NPE in Lint.augment
 * @modules java.compiler
 *          jdk.compiler
 * @compile Anno.java AnnoProcessor.java
 * @compile/fail/ref=LambdaInAnnotationsCausesNPETest2.out -XDrawDiagnostics -processor AnnoProcessor -proc:only LambdaInAnnotationsCausesNPETest2.java
 */

@Anno(value = (String x) -> x)
class LambdaInAnnotationsCausesNPETest2 {}
