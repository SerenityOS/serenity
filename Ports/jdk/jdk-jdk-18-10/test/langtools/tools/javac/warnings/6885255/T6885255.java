/**
 * @test /nodynamiccopyright/
 * @bug 6885255
 * @summary -Xlint:rawtypes
 * @compile/ref=T6885255.out -XDrawDiagnostics -Xlint:rawtypes T6885255.java
 */

class T6885255 {

    static class Test<X, Y> {}

    Class<Test> ct; //no warn - outer Class w/ raw param
    Class<Test<Test, Test>> ctt; //warn - outer Class w/o raw param (2)

    Class<Class<Test>> cct; //warn - outer Class w/o raw param
    Class<Class<Test<Test, Test>>> cctt; //warn - outer Class w/o raw param (2)

    Object o1 = (Test)null; //no warn - outer raw and cast
    Object o2 = (Test<Test, Test>)null; //warn - inner raw (2)

    Object o3 = (Class)null; //no warn - outer raw and cast
    Object o4 = (Class<Test>)null; //no warn - outer Class w/ raw param

    Object o5 = (Class<Test<Test, Test>>)null; //warn - outer Class w/ non raw param (2)
    Object o6 = (Class<Class<Test<Test, Test>>>)null; //warn - outer Class w/ non raw param (2)

    Object o7 = (Test<Class, Class>)null; //warn - inner raw (2)
    Object o8 = (Test<Class<Test>, Class<Test>>)null; //warn - inner Class (2)

    boolean b = null instanceof Test; //no warn - raw and instanceof
}
