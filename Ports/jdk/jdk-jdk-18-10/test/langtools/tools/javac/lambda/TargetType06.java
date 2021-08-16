/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check complex case of target typing
 * @author  Maurizio Cimadamore
 * @compile TargetType06.java
 */

import java.util.List;

class TargetType06 {

    class Foo {
        Foo getFoo() { return null; }
    }

    interface Function<A,R> {
        R invoke(A a);
    }

    static <B> List<B> map(Function<B, B> function) { return null; }

    void test() {
        List<Foo> l = map(foo -> foo.getFoo());
    }
}
