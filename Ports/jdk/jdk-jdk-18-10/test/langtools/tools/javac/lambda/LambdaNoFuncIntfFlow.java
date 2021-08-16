/**
 * @test /nodynamiccopyright/
 * @bug 8211102
 * @summary Ensure Flow does not crash for recovered lambdas
 * @compile/fail/ref=LambdaNoFuncIntfFlow.out -XDshould-stop.at=FLOW -XDrawDiagnostics LambdaNoFuncIntfFlow.java
 */

import java.util.*;

public class LambdaNoFuncIntfFlow {
    private void t(Object i) {
        int j = i instanceof ArrayList ? (ArrayList<String>) i : () -> { return null; };
        j = 0;
        Runnable r = () -> t(j);
    }
}
