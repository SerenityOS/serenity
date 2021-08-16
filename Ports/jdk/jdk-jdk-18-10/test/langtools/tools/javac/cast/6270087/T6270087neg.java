/*
 * @test    /nodynamiccopyright/
 * @bug     6270087 6932571
 * @summary Javac rejects legal cast
 * @compile/fail/ref=T6270087neg.out -XDrawDiagnostics T6270087neg.java
 */

class T6270087neg {

    static class Foo<X> {}

   <U extends Integer, V extends String> void test2(Foo<V> lv) {
        Object o = (Foo<U>) lv;
   }
}
