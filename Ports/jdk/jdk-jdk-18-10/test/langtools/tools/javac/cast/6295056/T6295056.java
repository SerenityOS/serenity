/*
 * @test    /nodynamiccopyright/
 * @bug     6295056
 * @summary Unchecked cast not reported as unsafe
 * @compile/ref=T6295056.out -XDrawDiagnostics -Xlint:unchecked T6295056.java
 * @compile T6295056.java
 */
public class T6295056 {
    interface Foo {}
    interface Bar<X> {}

    Object m(Foo f) {
        return (Bar<Object>)f;
    }

}
