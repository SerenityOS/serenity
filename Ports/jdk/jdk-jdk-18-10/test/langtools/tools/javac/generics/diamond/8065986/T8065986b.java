/*
 * @test /nodynamiccopyright/
 * @bug 8065986 8062373
 *
 * @summary Compiler fails to NullPointerException when calling super with Object<>()
 * @compile/fail/ref=T8065986b.out T8065986b.java -XDrawDiagnostics
 *
 */
import java.util.ArrayList;

class T8065986b {
    T8065986b() {
        this(new Object<>());
    }

    T8065986b(boolean b) {
        this(new ArrayList<>());
    }

    T8065986b(boolean b1, boolean b2) {
        this(()->{});
    }

    T8065986b(boolean b1, boolean b2, boolean b3) {
        this(T8065986b::m);
    }

    T8065986b(boolean cond, Object o1, Object o2) {
        this(cond ? o1 : o2);
    }

    T8065986b(int x) {
        this(new Object<>() {});
    }

    T8065986b(int x, int y) {
        this(new ArrayList<>() {});
    }
    static void m() { }
}
