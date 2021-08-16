/*
 * @test /nodynamiccopyright/
 * @bug 5009601
 * @summary enum constructors cannot be declared public or protected
 * @author Joseph D. Darcy
 *
 * @compile/fail/ref=EnumPublicConstructor.out -XDrawDiagnostics  EnumPublicConstructor.java
 */

enum EnumPublicConstructor {
    RED(255, 0, 0),
    GREEN(0, 255, 0),
    BLUE(0, 0, 255);

    private int r, g, b;
    public EnumPublicConstructor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
}
