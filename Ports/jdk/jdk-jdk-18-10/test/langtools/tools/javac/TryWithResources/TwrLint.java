/*
 * @test  /nodynamiccopyright/
 * @bug 6911256 6964740 6965277 6967065
 * @author Joseph D. Darcy
 * @summary Check that -Xlint:twr warnings are generated as expected
 * @compile/ref=TwrLint.out -Xlint:try,deprecation -XDrawDiagnostics TwrLint.java
 */

class TwrLint implements AutoCloseable {
    private static void test1() {
        try(TwrLint r1 = new TwrLint();
            TwrLint r2 = new TwrLint();
            TwrLint r3 = new TwrLint()) {
            r1.close();   // The resource's close
            r2.close(42); // *Not* the resource's close
            // r3 not referenced
        }

    }

    @SuppressWarnings("try")
    private static void test2() {
        try(@SuppressWarnings("deprecation") AutoCloseable r4 =
            new DeprecatedAutoCloseable()) {
            // r4 not referenced - but no warning is generated because of @SuppressWarnings
        } catch(Exception e) {
            ;
        }
    }

    /**
     * The AutoCloseable method of a resource.
     */
    @Override
    public void close () {
        return;
    }

    /**
     * <em>Not</em> the AutoCloseable method of a resource.
     */
    public void close (int arg) {
        return;
    }
}

@Deprecated
class DeprecatedAutoCloseable implements AutoCloseable {
    public DeprecatedAutoCloseable(){super();}

    @Override
    public void close () {
        return;
    }
}
