/*
 * @test  /nodynamiccopyright/
 * @bug 6827009
 * @summary Check for non-constant case labels.
 * @compile/fail/ref=NonConstantLabel.out -XDrawDiagnostics NonConstantLabel.java
 */
class NonConstantLabel {
    String m(String s) {
        String fauxConstant = "Goodbye Cruel World";
        switch(s) {
        case "Hello World":
            return(s);
        case fauxConstant:
            return (s + s);
        }
    }
}
