/*
 * @test /nodynamiccopyright/
 * @bug 8262891
 * @summary Check null handling for non-pattern switches.
 * @compile/fail/ref=CaseDefault.out -source 16 -Xlint:-options -XDrawDiagnostics CaseDefault.java
 * @compile --enable-preview -source ${jdk.version} CaseDefault.java
 * @run main/othervm --enable-preview CaseDefault
 */

public class CaseDefault {

    public static void main(String[] args) {
        new CaseDefault().run();
    }

    void run() {
        String str = "other";
        switch (str) {
            case "a": throw new AssertionError("Wrong branch.");
            case default: break; //OK
        }
        switch (str) {
            case "a" -> throw new AssertionError("Wrong branch.");
            case default -> {} //OK
        }
        int i;
        i = switch (str) {
            case "a": throw new AssertionError("Wrong branch.");
            case default: yield 0; //OK
        };
        i = switch (str) {
            case "a" -> throw new AssertionError("Wrong branch.");
            case default -> 0; //OK
        };
    }

}
