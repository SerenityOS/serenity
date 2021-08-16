/*
 * @test /nodynamiccopyright/
 * @ bug
 * @summary the type in an instanceof expression must be reifiable
 * @author seligman
 *
 * @compile/fail/ref=InstanceOf3.out -XDrawDiagnostics -source 15 -Xlint:-options InstanceOf3.java
 */

public class InstanceOf3 {
    boolean m() {
        return this.getClass() instanceof Class<? extends InstanceOf3>;
    }
}
