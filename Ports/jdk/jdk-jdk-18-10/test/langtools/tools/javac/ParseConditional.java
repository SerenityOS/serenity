/*
 * @test /nodynamiccopyright/
 * @bug 4092958
 * @summary The compiler was too permissive in its parsing of conditional
 *          expressions.
 * @author turnidge
 *
 * @compile/fail/ref=ParseConditional.out -XDrawDiagnostics ParseConditional.java
 */

public class ParseConditional {
    public static void main(String[] args) {
        boolean condition = true;
        int a = 1;
        int b = 2;
        int c = 3;
        int d = 4;
        // The following line should give an error because the conditional ?: operator
        // is higher priority than the final assignment operator, between c and d.
        // As such, the correct parsing is:
        //   a = (condition ? b = c : c) = d;
        // and it is illegal to try and assign to the value of the conditional expression.
        a = condition ? b = c : c = d;
    }
}
