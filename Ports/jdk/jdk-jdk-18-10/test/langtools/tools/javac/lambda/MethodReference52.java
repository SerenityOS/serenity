/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  special cases of method references (getClass()/Array.clone()) not handled properly
 * @compile/fail/ref=MethodReference52.out -XDrawDiagnostics MethodReference52.java
 */
import java.util.*;

class MethodReference52 {

    interface Clone1 {
        int[] m();
    }

    interface Clone2 {
        Object m();
    }

    interface WrongClone {
        long[] m();
    }

    interface GetClass {
        Class<? extends List> m();
    }

    interface WrongGetClass {
        Class<List<String>> m();
    }

    void test(int[] iarr, List<String> ls) {
        Clone1 c1 = iarr::clone; //ok
        Clone2 c2 = iarr::clone; //ok - type more generic
        WrongClone c3 = iarr::clone; //bad return type
        GetClass c4 = ls::getClass; //ok
        WrongGetClass c5 = ls::getClass; //bad return type
    }
}
