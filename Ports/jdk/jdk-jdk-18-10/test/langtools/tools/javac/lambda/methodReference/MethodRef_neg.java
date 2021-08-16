/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *   This is negative test for wrong parameter/return type in method references
 * @compile/fail/ref=MethodRef_neg.out -XDrawDiagnostics MethodRef_neg.java
 */

public class MethodRef_neg {

    static interface A {void m(Integer i);}

    static interface B {void m(String s);}

    static interface C {Integer m();}

    static interface D {String m();}


    static void bar(int x) { }

    int foo() {
        return 5;
    }

    static void make() { }

    void method() {
        A a = MethodRef_neg::bar; //boxing on parameter type is ok
        B b = MethodRef_neg::bar; //wrong parameter type, required: String, actual: int
        C c = this::foo; //boxing on return type is ok
        D d = this::foo; //wrong return type, required: String, actual: int
        a = MethodRef_neg::make; //missing parameter
        c = MethodRef_neg::make; //missing return type
    }
}
