/*
 * @test /nodynamiccopyright/
 * @bug 8231827
 * @summary Clashing bindings are reported correctly
 * @compile/fail/ref=BindingsExistTest.out -XDrawDiagnostics BindingsExistTest.java
 */
public class BindingsExistTest {
    public void t(Object o1, Object o2) {
        if (o1 instanceof String k && o2 instanceof Integer k) {}

        if (o1 instanceof String k || o2 instanceof Integer k) {}

        if (!(o1 instanceof String k)) {
            return ;
        }
        if (o1 instanceof Integer k) {}

        String s2 = "";
        if (o1 instanceof String s2) {}

        if (o1 instanceof String s3) {
            String s3 = "";
        }

        if (!(o1 instanceof String s4)) {
            return ;
        }
        String s4 = "";
    }
}
