/*
 * @test /nodynamiccopyright/
 * @bug 8231827
 * @summary Testing pattern matching against the null constant
 * @compile/fail/ref=NullsInPatterns.out -XDrawDiagnostics NullsInPatterns.java
 */
import java.util.List;

public class NullsInPatterns {

    public static void main(String[] args) {
        if (null instanceof List t) {
            throw new AssertionError("broken");
        } else {
            System.out.println("null does not match List type pattern");
        }
        //reifiable types not allowed in type test patterns in instanceof:
//        if (null instanceof List<Integer> l) {
//            throw new AssertionError("broken");
//        } else {
//            System.out.println("null does not match List<Integer> type pattern");
//        }
        if (null instanceof List<?> l) {
            throw new AssertionError("broken");
        } else {
            System.out.println("null does not match List<?> type pattern");
        }
    }
}
