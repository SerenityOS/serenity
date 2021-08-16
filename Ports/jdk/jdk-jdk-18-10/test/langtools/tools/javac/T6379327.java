/*
 * @test    /nodynamiccopyright/
 * @bug     6379327
 * @summary Erroneous catch block not detected with anonymous class declaration
 * @author  Peter Jones, Wei Tao
 * @compile/fail/ref=T6379327.out -XDrawDiagnostics T6379327.java
 */

import java.security.*;
public class T6379327 {
    public static void main(String[] args) {
        final String name = args[0];
        try {
            new PrivilegedExceptionAction() {
                public Object run() throws ClassNotFoundException {
                    return Class.forName(name);
                }
            };
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
