/*
 * @test    /nodynamiccopyright/
 * @bug     6302956
 * @summary Illegal cast allowed Properties -> Map<String, String>
 * @compile/fail/ref=T6302956.out -XDrawDiagnostics  T6302956.java
 */

import java.util.Map;

public class T6302956 {
    Object test() {
        return (Map<String, String>)System.getProperties();
    }
}
