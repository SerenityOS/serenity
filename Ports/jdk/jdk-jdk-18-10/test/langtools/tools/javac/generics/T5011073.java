/*
 * @test /nodynamiccopyright/
 * @bug 5011073
 * @summary javac should implement JLS3 three-pass overload resolution
 * @author gafter
 *
 * @compile/fail/ref=T5011073.out -XDrawDiagnostics   T5011073.java
 */

import java.util.*;
class T5011073 {
    static void f(Set<String> s, Class<String> c) {}

    static void g(Set<Integer> s, Class c) {
        f(s, c);
    }
}
