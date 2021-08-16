/*
 * @test  /nodynamiccopyright/
 * @bug 6827009
 * @summary Check for case labels of different types.
 * @compile/fail/ref=BadlyTypedLabel1.out -XDrawDiagnostics BadlyTypedLabel1.java
 */
class BadlyTypedLabel1 {
    String m(String s) {
        switch(s) {
        case "Hello World":
            return(s);
        case 42:
            return ("Don't forget your towel!");
        }
    }
}
