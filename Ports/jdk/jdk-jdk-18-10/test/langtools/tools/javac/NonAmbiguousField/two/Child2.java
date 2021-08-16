/* /nodynamiccopyright/ */

package two;

interface I {
    int i = 11;
}

public class Child2 extends one.Parent2 implements I {
    void method() {
        System.out.println(i);
    }
}
