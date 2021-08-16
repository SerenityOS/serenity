/* @test /nodynamiccopyright/
 * @bug 7196163
 * @summary Verify that variables can be used as operands to try-with-resources
 * @compile/fail/ref=TwrForVariable1.out -source 8 -XDrawDiagnostics -Xlint:-options TwrForVariable1.java
 * @compile TwrForVariable1.java
 * @run main TwrForVariable1
 */
public class TwrForVariable1 implements AutoCloseable {
    private static int closeCount = 0;
    public static void main(String... args) {
        TwrForVariable1 v = new TwrForVariable1();

        try (v) {
            assertCloseCount(0);
        }
        try (/**@deprecated*/v) {
            assertCloseCount(1);
        }
        try (v.finalWrapper.finalField) {
            assertCloseCount(2);
        } catch (Exception ex) {
        }
        try (new TwrForVariable1() { }.finalWrapper.finalField) {
            assertCloseCount(3);
        } catch (Exception ex) {
        }
        try ((args.length > 0 ? v : new TwrForVariable1()).finalWrapper.finalField) {
            assertCloseCount(4);
        } catch (Exception ex) {
        }
        try {
            throw new CloseableException();
        } catch (CloseableException ex) {
            try (ex) {
                assertCloseCount(5);
            }
        }

        assertCloseCount(6);

        // null test cases
        TwrForVariable1 n = null;

        try (n) {
        }
        try (n) {
            throw new Exception();
        } catch (Exception e) {
        }

        assertCloseCount(6);

        // initialization exception
        TwrForVariable1 i1 = new TwrForVariable1();
        try (i1; TwrForVariable1 i2 = new TwrForVariable1(true)) {
        } catch (Exception e) {
        }

        assertCloseCount(7);

        // multiple closures
        TwrForVariable1 m1 = new TwrForVariable1();
        try (m1; TwrForVariable1 m2 = m1; TwrForVariable1 m3 = m2;) {
        }

        assertCloseCount(10);

        // aliasing of variables keeps equality (bugs 6911256 6964740)
        TwrForVariable1 a1 = new TwrForVariable1();
        try (a1; TwrForVariable1 a2 = a1;) {
            if (a2 != a2)
                throw new RuntimeException("Unexpected inequality.");
        }

        assertCloseCount(12);

        // anonymous class implementing AutoCloseable as variable in twr
        AutoCloseable a = new AutoCloseable() {
            public void close() { };
        };
        try (a) {
        } catch (Exception e) {}
    }

    static void assertCloseCount(int expectedCloseCount) {
        if (closeCount != expectedCloseCount)
            throw new RuntimeException("bad closeCount: " + closeCount +
                                       "; expected: " + expectedCloseCount);
    }

    public void close() {
        closeCount++;
    }

    final FinalWrapper finalWrapper = new FinalWrapper();

    static class FinalWrapper {
        public final AutoCloseable finalField = new AutoCloseable() {
            @Override
            public void close() throws Exception {
                closeCount++;
            }
        };
    }

    static class CloseableException extends Exception implements AutoCloseable {
        @Override
        public void close() {
            closeCount++;
        }
    }

    public TwrForVariable1(boolean throwException) {
        if (throwException)
            throw new RuntimeException("Initialization exception");
    }

    public TwrForVariable1() {
        this(false);
    }
}
