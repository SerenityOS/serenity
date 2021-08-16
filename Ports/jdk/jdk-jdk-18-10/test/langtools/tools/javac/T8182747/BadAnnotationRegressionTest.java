/* @test /nodynamiccopyright/
 * @bug 8182747
 * @summary javac crashes on bad annotation value
 * @compile/fail/ref=BadAnnotationRegressionTest.out -XDrawDiagnostics BadAnnotationRegressionTest.java
 */

class BadAnnotationRegressionTest {
    @interface ClassAnno {
        Class<?> value();
    }

    @interface ArrayAnno {
        int[] value();
    }

    @interface PrimitiveAnno {
        int value();
    }

    @interface StringAnno {
        String value();
    }

    enum E {
        A,
        B,
    }

    @interface EnumAnno {
        E value();
    }

    static final Class<?> c = Object.class;
    static final int i = 0;
    static final int[] arr = new int[] { 1, 2, 3 };
    static final E a = E.A;
    static final String s = "";

    @ClassAnno(c)     // error
    @PrimitiveAnno(i) // ok
    @ArrayAnno(arr)   // error
    @EnumAnno(a)      // error
    @StringAnno(s)    //ok
    void testAnno() {}
}
