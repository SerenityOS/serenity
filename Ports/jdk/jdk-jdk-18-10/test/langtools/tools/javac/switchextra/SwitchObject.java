/*
 * @test /nodynamiccopyright/
 * @bug 8206986
 * @summary Verify switch over Object is not allowed.
 * @compile/fail/ref=SwitchObject.out -XDrawDiagnostics SwitchObject.java
 */
public class SwitchObject {

    private int longSwitch(Object o) {
        switch (o) {
            case -1: return 0;
            case "": return 1;
            default: return 3;
        }
    }

}
