/**
 * @test /nodynamiccopyright/
 * @bug 8230105
 * @summary Verify analyzers work reasonably in combination with mandatory warnings
 * @compile/ref=AnalyzerMandatoryWarnings.out -Xlint:deprecation -XDrawDiagnostics -Xmaxwarns 1 -XDfind=lambda AnalyzerMandatoryWarnings.java
 */
public class AnalyzerMandatoryWarnings {
    private void test() {
        Runnable r = new Runnable() {
            public void run() {
                Depr r;
            }
        };
    }
}
@Deprecated class Depr {}
