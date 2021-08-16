/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  Structural most specific doesn't handle cases with wildcards in functional interfaces
 * @compile/fail/ref=MostSpecific05.out -XDrawDiagnostics MostSpecific05.java
 */
public class MostSpecific05 {

    interface ObjectConverter<T extends Object> {
        T map(Object o);
    }

    interface NumberConverter<T extends Number> {
        T map(Object o);
    }

    static class MyMapper<A extends Object, B extends Number> {
        void map(ObjectConverter<? extends A> m) { }
        void map(NumberConverter<? extends B> m) { }
    }

    public static void main(String[] args) {
        MyMapper<Number, Double> mm = new MyMapper<Number, Double>();
        mm.map(e->1.0); //ambiguous - implicit
        mm.map((Object e)->1.0); //ok
    }
}
