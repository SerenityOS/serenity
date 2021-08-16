/*
 * @test /nodynamiccopyright/
 * @bug 8257307
 * @summary No javac warning when calling deprecated constructor with diamond
 * @run compile/ref=T8257037.out -Xlint -XDrawDiagnostics T8257037.java
 */

public class T8257037 {
    T8257037_GenericClass<Object> test = new T8257037_GenericClass<>(); // use diamond
}

class T8257037_GenericClass<T> {
    @Deprecated
    public T8257037_GenericClass() {}
}
