/*
 * @test /nodynamiccopyright/
 * @bug 4986256
 * @compile/ref=DepAnn.out -XDrawDiagnostics -Xlint:all DepAnn.java
 */

// control: this class should generate warnings
/** @deprecated */
class DepAnn
{
    /** @deprecated */
    void m1(int i) {
    }
}

// tests: the warnings that would otherwise be generated should all be suppressed
@SuppressWarnings("dep-ann")
/** @deprecated */
class DepAnn1
{
    /** @deprecated */
    void m1(int i) {
        /** @deprecated */
        int x = 3;
    }
}

class DepAnn2
{
    @SuppressWarnings("dep-ann")
    /** @deprecated */
    class Bar {
        /** @deprecated */
        void m1(int i) {
        }
    }

    @SuppressWarnings("dep-ann")
    /** @deprecated */
    void m2(int i) {
    }


    @SuppressWarnings("dep-ann")
    static int x = new DepAnn2() {
            /** @deprecated */
            int m1(int i) {
                return 0;
            }
        }.m1(0);

}

// this class should produce warnings because @SuppressWarnings should not be inherited
/** @deprecated */
class DepAnn3 extends DepAnn1
{
    /** @deprecated */
    void m1(int i) {
    }
}
