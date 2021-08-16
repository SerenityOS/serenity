/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  Structural most specific doesn't handle cases with wildcards in functional interfaces
 * @compile/fail/ref=MostSpecific04.out -XDrawDiagnostics MostSpecific04.java
 */
public class MostSpecific04 {

    interface DoubleMapper<T> {
        double map(T t);
    }

    interface LongMapper<T> {
        long map(T t);
    }

    static class MyList<E> {
        void map(DoubleMapper<? super E> m) { }
        void map(LongMapper<? super E> m) { }
    }

    public static void main(String[] args) {
        MyList<String> ls = new MyList<String>();
        ls.map(e->e.length()); //ambiguous - implicit
        ls.map((String e)->e.length()); //ok
    }
}
