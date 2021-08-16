/*
 * @test /nodynamiccopyright/
 * @bug 8206986 8226510
 * @summary Verify than a switch that does not yield a value is rejected.
 * @compile/fail/ref=EmptySwitch.out -XDrawDiagnostics -XDshould-stop.at=FLOW EmptySwitch.java
 */

public class EmptySwitch {
    private void print(EmptySwitchEnum t) {
        (switch (t) {
        }).toString();
        (switch (t) {
            default -> throw new IllegalStateException();
        }).toString();
        (switch (t) {
            default: throw new IllegalStateException();
        }).toString();
        (switch (0) {
            case 0: yield "";
            default:
        }).toString();
        (switch (0) {
            case 0 -> { yield ""; }
            default -> { }
        }).toString();
    }

    enum EmptySwitchEnum {
    }
}
