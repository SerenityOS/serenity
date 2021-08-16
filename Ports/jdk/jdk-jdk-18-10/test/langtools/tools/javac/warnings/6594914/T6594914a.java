/**
 * @test /nodynamiccopyright/
 * @bug 6594914
 * @summary \\@SuppressWarnings("deprecation") does not not work for the type of a variable
 * @compile/ref=T6594914a.out -XDrawDiagnostics -Xlint:deprecation T6594914a.java
 */


class T6747671a {

    DeprecatedClass a1; //warn

    @SuppressWarnings("deprecation")
    DeprecatedClass a2;

    <X extends DeprecatedClass> DeprecatedClass m1(DeprecatedClass a)
            throws DeprecatedClass { return null; } //warn

    @SuppressWarnings("deprecation")
    <X extends DeprecatedClass> DeprecatedClass m2(DeprecatedClass a)
            throws DeprecatedClass { return null; }

    void test() {
        DeprecatedClass a1; //warn

        @SuppressWarnings("deprecation")
        DeprecatedClass a2;
    }
}
