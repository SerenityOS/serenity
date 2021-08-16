/*
 * @test /nodynamiccopyright/
 * @bug 6970584 7060926
 * @summary Attr.PostAttrAnalyzer misses a case
 *
 * @compile/fail/ref=FailOver15.out -XDrawDiagnostics --should-stop=at=FLOW -XDdev FailOver15.java
 */

class Test {
    void m() {
        new UnknownClass<String, Void>() {
            public String getString() {
                String s = "";
                s += "more";
                return s;
            }
        }
    }
}
