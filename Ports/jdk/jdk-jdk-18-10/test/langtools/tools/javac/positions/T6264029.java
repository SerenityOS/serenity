/*
 * @test  /nodynamiccopyright/
 * @bug     6264029
 * @summary Compiler issues wrong unchecked warning for anonymous inner class
 * @author  Seetharama Avadhanam
 * @compile -Xlint:unchecked -XDdev T6264029.java
 * @compile/ref=T6264029.out -Xlint:unchecked -XDdev -XDrawDiagnostics T6264029.java
 */

class T6264029A<T,K> {
    public T6264029A(K k) {}
}

public class T6264029 {
    T6264029A a = new T6264029A("saaa") {
            public void getNoneTest() {}
        };
}
