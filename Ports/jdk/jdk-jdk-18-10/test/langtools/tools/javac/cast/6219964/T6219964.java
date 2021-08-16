/*
 * @test    /nodynamiccopyright/
 * @bug     6219964 8161013
 * @summary Anonymous class types are not final
 * @compile T6219964.java
 */

public class T6219964 {
    interface I { }
    void foo() {
        new Object() {
            I bar() {
                return (I)this;
            }
        };
    }
}
