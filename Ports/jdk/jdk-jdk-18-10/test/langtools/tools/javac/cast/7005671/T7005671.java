/*
 * @test /nodynamiccopyright/
 * @author mcimadamore
 * @bug     7005671
 * @summary Regression: compiler accepts invalid cast from X[] to primitive array
 * @compile/fail/ref=T7005671.out -XDrawDiagnostics T7005671.java
 */

class T7005671<X> {

    void test1() {
        Object o1 = (X[])(byte[])null;
        Object o2 = (X[])(short[])null;
        Object o3 = (X[])(int[])null;
        Object o4 = (X[])(long[])null;
        Object o5 = (X[])(float[])null;
        Object o6 = (X[])(double[])null;
        Object o7 = (X[])(char[])null;
        Object o8 = (X[])(boolean[])null;
    }

    void test2() {
        Object o1 = (byte[])(X[])null;
        Object o2 = (short[])(X[])null;
        Object o3 = (int[])(X[])null;
        Object o4 = (long[])(X[])null;
        Object o5 = (float[])(X[])null;
        Object o6 = (double[])(X[])null;
        Object o7 = (char[])(X[])null;
        Object o8 = (boolean[])(X[])null;
    }
}
