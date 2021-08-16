/*
 * @test /nodynamiccopyright/
 * @bug 4982096 5004321
 * @summary the type in an instanceof expression must be reifiable
 * @author seligman
 *
 * @compile/fail/ref=InstanceOf2.out -XDrawDiagnostics  InstanceOf2.java
 */

public class InstanceOf2 {
    boolean m() {
        return this.getClass() instanceof Class<InstanceOf2>;
    }
}
