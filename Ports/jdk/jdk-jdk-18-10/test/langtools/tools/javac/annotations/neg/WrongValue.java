/*
 * @test /nodynamiccopyright/
 * @bug 4951298
 * @summary compiler: crashes when attributes with same elements are used in place of other
 * @author gafter
 *
 * @compile/fail/ref=WrongValue.out -XDrawDiagnostics  WrongValue.java
 */

@interface TestM2 {
    String message() default "MyMessage";
    Class myClass() default Integer.class;
}

@interface TestM3 {
    String message() default "MyMessage";
    Class myClass() default Integer.class;

}

@interface X {
    TestM2 value();
}

@X(@TestM3())
class Foo {
}
