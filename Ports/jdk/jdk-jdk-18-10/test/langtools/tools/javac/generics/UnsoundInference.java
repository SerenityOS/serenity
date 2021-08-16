/*
 * @test /nodynamiccopyright/
 * @bug 5020448
 * @summary Generic method allowing passing of types that don't match collection types
 * @author gafter
 *
 * @compile/fail/ref=UnsoundInference.out -XDrawDiagnostics  UnsoundInference.java
 */

import java.util.ArrayList;
import java.util.Collection;

public class UnsoundInference {

    public static void main(String[] args) {
        Object[] objArray = {new Object()};
        ArrayList<String> strList = new ArrayList<String>();
        transferBug(objArray, strList);
        String str = strList.get(0);
    }

    public static <Var> void transferBug(Var[] from, Collection<Var> to) {
        to.add(from[0]);
    }
}
