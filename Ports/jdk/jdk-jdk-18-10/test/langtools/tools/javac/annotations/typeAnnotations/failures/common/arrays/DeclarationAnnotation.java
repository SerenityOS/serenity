/*
 * @test /nodynamiccopyright/
 * @bug 8013852
 * @summary ensure that declaration annotations are not allowed on
 *   new array expressions
 * @author Werner Dietl
 * @compile/fail/ref=DeclarationAnnotation.out -XDrawDiagnostics DeclarationAnnotation.java
 */
class DeclarationAnnotation {
    Object e1 = new @DA int[5];
    Object e2 = new @DA String[42];
    Object e3 = new @DA Object();
    Object e4 = new @DA Object() { };
}

@interface DA { }
