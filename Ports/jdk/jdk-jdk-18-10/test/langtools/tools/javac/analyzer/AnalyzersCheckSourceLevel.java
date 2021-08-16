/*
 * @test /nodynamiccopyright/
 * @bug 8211102
 * @summary Ensure that the lambda analyzer does not run when -source 7 is specified,
 *          even if explicitly requested
 * @compile/fail/ref=AnalyzersCheckSourceLevel.out -Werror -XDfind=lambda -XDrawDiagnostics AnalyzersCheckSourceLevel.java
 * @compile -Werror -source 7 -Xlint:-options -XDfind=lambda AnalyzersCheckSourceLevel.java
 */
public class AnalyzersCheckSourceLevel {
    void t() {
        Runnable r = new Runnable() {
            @Override public void run() {}
        };
    }
}