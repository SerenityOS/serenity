/*
 * @test  /nodynamiccopyright/
 * @bug 6827009
 * @summary Check for repeated string case labels.
 * @compile/fail/ref=RSCL2.out -XDrawDiagnostics RepeatedStringCaseLabels2.java
 */
class RepeatedStringCaseLabels2 {
    String m(String s) {
        final String constant = "Hello" + " " + "World";
        switch(s) {
        case "Hello World":
            return(s);
        case constant:
            return (s + s);
        }
    }
}
