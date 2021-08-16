/*
 * @test /nodynamiccopyright/
 * @bug 8008276
 * @summary assertion error in com.sun.tools.javac.comp.TransTypes.visitApply
 * @compile/fail/ref=MissingError.out -XDrawDiagnostics MissingError.java
 */
class MissingError {
    void test() {
       mtest(new Bad(){ Integer i = ""; });
    }

    void mtest(Bad t){ }
}

class Bad {
    String s = 1;
}
