/**
 * @test /nodynamiccopyright/
 * @bug 8222850
 * @summary Check error recovery for switch over undefined variables.
 * @compile/fail/ref=SwitchUndefinedSelector.out -XDrawDiagnostics --should-stop=at=FLOW SwitchUndefinedSelector.java
 */

public class SwitchUndefinedSelector {
    private static final Object D = null;
    public void switchTest() {
        switch (undefined) {
            case A -> {}
            case B, C -> {}
            case D -> {}
        }
        var v = switch (undefined) {
            case A -> 0;
            case B, C -> 0;
            case D -> 0;
        };
        switch (undefined) {
            case SwitchUndefinedSelector.D -> {}
            case SwitchUndefinedSelector.UNDEF -> {}
        }
    }
}
