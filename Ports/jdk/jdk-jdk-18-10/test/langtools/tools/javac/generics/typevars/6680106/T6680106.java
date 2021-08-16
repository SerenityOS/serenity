/*
 * @test /nodynamiccopyright/
 * @bug     6680106
 * @summary StackOverFlowError for Cyclic inheritance in TypeParameters with ArrayType Bounds
 * @author  Maurizio Cimadamore
 * @compile/fail/ref=T6680106.out -XDrawDiagnostics T6680106.java
 */

class T6680106 {
    class A0 {}
    class A1<T extends T[]> {}
    class A2<T extends S[], S extends T[]> {}
    class A3<T extends S[], S extends U[], U extends T[]> {}
    class A5<T extends A0 & T[]> {}
    class A6<T extends A0 & S[], S extends A0 & T[]> {}
    class A7<T extends A0 & S[], S extends A0 & U[], U extends A0 & T[]> {}
}
