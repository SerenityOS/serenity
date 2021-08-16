/*
 * @test /nodynamiccopyright/
 * @bug 8231827
 * @summary Basic pattern bindings scope test
 * @compile/fail/ref=MatchBindingScopeTest.out -XDrawDiagnostics MatchBindingScopeTest.java
 */
public class MatchBindingScopeTest {

    static Integer i = 42;
    static String s = "Hello";
    static Object o1 = s;
    static Object o2 = i;

    public static void main(String[] args) {

        if (o1 instanceof String j && j.length() == 5) { // OK
            System.out.println(j); // OK
        } else {
            System.out.println(j); // NOT OK
        }

        // NOT OK, name reused.
        if (o1 instanceof String j && o2 instanceof Integer j) {
        }

        if (o1 instanceof String j && j.length() == 5 && o2 instanceof Integer k && k == 42) { // OK
            System.out.println(j); // OK
            System.out.println(k); // OK
        } else {
            System.out.println(j); // NOT OK
            System.out.println(k); // NOT OK
        }

        if (o1 instanceof String j || j.length() == 5) { // NOT OK
            System.out.println(j); // NOT OK
        }

        if (o1 instanceof String j || o2 instanceof Integer j) { // NOT OK, types differ
            System.out.println(j);
        } else {
            System.out.println(j); // NOT OK.
        }

        while (o1 instanceof String j && j.length() == 5) { // OK
            System.out.println(j); // OK
        }

        while (o1 instanceof String j || true) {
            System.out.println(j); // Not OK
        }

        for (; o1 instanceof String j; j.length()) { // OK
            System.out.println(j); // OK
        }

        for (; o1 instanceof String j || true; j.length()) { // NOT OK
            System.out.println(j); // Not OK
        }

        int x = o1 instanceof String j ?
                      j.length() : // OK.
                      j.length();  // NOT OK.

        x = !(o1 instanceof String j) ?
                      j.length() : // NOT OK.
                      j.length();  // OK.
    }
}
