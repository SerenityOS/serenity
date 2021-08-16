/*
 * @test /nodynamiccopyright/
 * @bug 7085024
 * @summary internal error; cannot instantiate Foo
 * @compile/fail/ref=T7085024.out -XDrawDiagnostics T7085024.java
 */

class T7085024 {
    T7085024 (boolean ret) { } //internal error goes away if constructor accepts a reference type

    T7085024 f = new T7085024((NonExistentClass) null );
}
