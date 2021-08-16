/**
 * @test    /nodynamiccopyright/
 * @bug     6384510
 * @summary improper handling of wildcard captures
 * @author  Martin Buchholz
 * @compile/fail/ref=Martin.out -XDrawDiagnostics  Martin.java
 */

import java.util.List;

public class Martin {
    public static void main(String[] args) throws Throwable {
        List<?> x1 = null;
        List<?> x2 = null;
        x1.addAll(x2);
    }
}
