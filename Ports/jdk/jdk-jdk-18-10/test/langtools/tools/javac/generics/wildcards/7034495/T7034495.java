/*
 * @test    /nodynamiccopyright/
 * @bug     7034495
 * @summary Javac asserts on usage of wildcards in bounds
 * @compile/fail/ref=T7034495.out -XDrawDiagnostics T7034495.java
 */
class T7034495 {

    interface A<T> {
        T foo();
    }

    interface B<T> {
        T foo();
    }

    interface C<T extends A<?> & B<?>> { }

}
