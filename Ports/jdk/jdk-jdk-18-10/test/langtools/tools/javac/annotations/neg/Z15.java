/*
 * @test /nodynamiccopyright/
 * @bug 4865660
 * @summary implement "metadata" (attribute interfaces and program annotations)
 * @author gafter
 *
 * @compile/fail/ref=Z15.out -XDrawDiagnostics  Z15.java
 */

@interface An {
    String a() default "foo".intern();
}
