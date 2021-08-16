/*
 * @test /nodynamiccopyright/
 * @bug 4919255 4982096 5004321
 * @summary the type of x.getClass() is no longer Class<? extends X>
 * @author gafter
 *
 * @compile/fail/ref=GetClass.out -XDrawDiagnostics  GetClass.java
 */

public class GetClass {
    public static void main(String[] args) {
        Class<? extends Class<GetClass>> x = GetClass.class.getClass();
    }
}
