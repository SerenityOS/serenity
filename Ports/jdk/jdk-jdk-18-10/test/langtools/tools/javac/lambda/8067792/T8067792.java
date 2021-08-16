/*
 * @test /nodynamiccopyright/
 * @bug 8067792
 * @summary Javac crashes in finder mode with nested implicit lambdas
 * @compile/fail/ref=T8067792.out -XDrawDiagnostics -Werror -XDfind=lambda T8067792.java
 */

import java.util.stream.*;
import java.util.*;

class T8067792 {
    void test(Stream<List<?>> sl) {
        Runnable r = new Runnable() {
            public void run() {
                Stream<List<?>> constructor = sl.filter(c -> true);
            }
        };
    }
}
