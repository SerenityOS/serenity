/*
 * @test    /nodynamiccopyright/
 * @bug     6495506
 * @summary Cast inserted by generics can cause IllegalAccessError
 * @compile A.java
 * @compile/fail/ref=T6495506.out -XDrawDiagnostics  T6495506.java
 */

public class T6495506 {
    public static void main(String... args) {
        a.A myA = new a.A();
        myA.p = myA.vec.get(0);
    }
}
