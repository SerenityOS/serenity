/**
 * @test /nodynamiccopyright/
 * @bug 6470588
 * @summary Verify that \\@SuppressWarnings("deprecation") works OK for all parts
 *          of class/method/field "header", including (declaration) annotations
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 * @build VerifySuppressWarnings
 * @compile/ref=T6480588.out -XDrawDiagnostics -Xlint:unchecked,deprecation,cast T6480588.java
 * @run main VerifySuppressWarnings T6480588.java
 */
// TODO: 8057683 improve ordering of errors with type annotations
@DeprecatedAnnotation
class T6480588 extends DeprecatedClass implements DeprecatedInterface {
    @DeprecatedAnnotation
    public DeprecatedClass method(DeprecatedClass param) throws DeprecatedClass {
        DeprecatedClass lv = new DeprecatedClass();
        @Deprecated
        DeprecatedClass lvd = new DeprecatedClass();
        return null;
    }

    @Deprecated
    public void methodD() {
    }

    @DeprecatedAnnotation
    DeprecatedClass field = new DeprecatedClass();

    @DeprecatedAnnotation
    class Inner extends DeprecatedClass implements DeprecatedInterface {
    }

}

@Deprecated class DeprecatedClass extends Throwable { }
@Deprecated interface DeprecatedInterface { }
@Deprecated @interface DeprecatedAnnotation { }
