/*
 * @test /nodynamiccopyright/
 * @bug 8268320
 * @summary Verify user-friendly errors are reported for ill-formed pattern.
* @compile/fail/ref=PatternErrorRecovery.out -XDrawDiagnostics -XDshould-stop.at=FLOW --enable-preview -source ${jdk.version} PatternErrorRecovery.java
 * @compile/fail/ref=PatternErrorRecovery-no-preview.out -XDrawDiagnostics -XDshould-stop.at=FLOW PatternErrorRecovery.java
 */
public class PatternErrorRecovery {
    void errorRecoveryNoPattern1(Object o) {
        switch (o) {
            case String: break;
            case Object obj: break;
        }
    }
}
