/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that diagnostics on nested erroneous deferred types are flushed
 * @compile/fail/ref=BadMethodCall.out -XDrawDiagnostics BadMethodCall.java
 */
import java.util.*;

class BadMethodCall {
    <I> List<I> id(List<I> z) { return null; };

    List<String> cons(String s, List<String> ls) { return null; }

    void test(List<Object> lo) { Object t = cons(id(""),lo); }
}
