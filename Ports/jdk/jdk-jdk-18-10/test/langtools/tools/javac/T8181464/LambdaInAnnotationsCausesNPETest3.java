/* @test /nodynamiccopyright/
 * @bug 8181464
 * @summary Invalid lambda in annotation causes NPE in Lint.augment
 * @modules java.compiler
 *          jdk.compiler
 * @compile Anno2.java AnnoProcessor.java
 * @compile/fail/ref=LambdaInAnnotationsCausesNPETest3.out -XDrawDiagnostics -processor AnnoProcessor -proc:only LambdaInAnnotationsCausesNPETest3.java
 */

@Anno2(value = LambdaInAnnotationsCausesNPETest3.m(x -> x))
class LambdaInAnnotationsCausesNPETest3 {
    static String m(Class<?> target) {
        return null;
    }
}
