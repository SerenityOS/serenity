/*
 * @test /nodynamiccopyright/
 * @bug 8043893
 * @summary Inference doesn't report error on incompatible upper bounds
 *
 * @compile -source 7 T8043893.java
 * @compile/fail/ref=T8043893.out -Xlint:-options -XDrawDiagnostics -source 8 T8043893.java
 */

class T8043893<X> {

    interface S1<U> { }

    interface S2<U> { }

    interface T0 { }

    interface T1 extends S1<Number>, S2<Number> { }

    interface T2 extends S1<Integer>, S2<Integer> { }

    interface T3 extends S1<Number>, S2<Integer> { }

    interface T4 extends S1<Number> { }

    interface T5 extends S1<Integer> { }

    <Z extends T1> void m_intersection(T8043893<? super Z> a) { }

    <Z extends T4> void m_class(T8043893<? super Z> a) { }

    void test() {
        //intersection type checks
        m_intersection(new T8043893<T1>()); //ok in both 7 and 8 - Z = T1
        m_intersection(new T8043893<T2>()); //ok in 7, fails in 8
        m_intersection(new T8043893<T3>()); //ok in 7, fails in 8
        m_intersection(new T8043893<T0>()); //ok in both 7 and 8 - Z = T0 & T1
        //class type checks
        m_class(new T8043893<T4>()); //ok in both 7 and 8 - Z = T4
        m_class(new T8043893<T5>()); //ok in 7, fails in 8
        m_class(new T8043893<T0>()); //ok in both 7 and 8 - Z = T0 & T4
    }
}
