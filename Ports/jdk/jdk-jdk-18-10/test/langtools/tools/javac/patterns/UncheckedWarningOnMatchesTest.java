/*
 * @test /nodynamiccopyright/
 * @bug 8187429 8231827
 * @summary Missing unchecked conversion warning
 * @compile/fail/ref=UncheckedWarningOnMatchesTest.out -Xlint:unchecked -Werror -XDrawDiagnostics UncheckedWarningOnMatchesTest.java
 */
import java.util.ArrayList;

public class UncheckedWarningOnMatchesTest {

    public static void main(String [] args) {

        Object o = new ArrayList<UncheckedWarningOnMatchesTest>();
        if (o instanceof ArrayList<Integer> ai) {  // unchecked conversion
            System.out.println("Blah");
        }
    }
}
