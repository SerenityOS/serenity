/*
 * @test  /nodynamiccopyright/
 * @bug 8025113
 * @author sogoel
 * @summary Resources cannot be declared outside t-w-r block
 * @compile/fail/ref=ResDeclOutsideTry.out -XDrawDiagnostics ResDeclOutsideTry.java
 */

public class ResDeclOutsideTry implements AutoCloseable {
    ResDeclOutsideTry tr1;
    ResDeclOutsideTry tr2 = new ResDeclOutsideTry();

    String test1() {
        try (tr1 = new ResDeclOutsideTry(); tr2;) {
        }
        return null;
    }

    @Override
    public void close() throws Exception {
    }
}

