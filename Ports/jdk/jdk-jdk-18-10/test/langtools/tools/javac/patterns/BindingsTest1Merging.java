/*
 * @test /nodynamiccopyright/
 * @bug 8231827
 * @summary Basic tests for bindings from instanceof - tests for merging pattern variables
 * @compile/fail/ref=BindingsTest1Merging.out -XDrawDiagnostics BindingsTest1Merging.java
 */

public class BindingsTest1Merging {
    public static boolean Ktrue() { return true; }
    public static void main(String[] args) {
        Object o1 = "hello";
        Integer i = 42;
        Object o2 = i;
        Object o3 = "there";

        // Test for e1 && e2.F = intersect(e1.F, e2.F)
        if (!(o1 instanceof String s) && !(o1 instanceof String s)) {

        } else {
            s.length();
        }

        // Test for (e1 || e2).T = intersect(e1.T, e2.T)
        if (o1 instanceof String s || o3 instanceof String s){
            System.out.println(s); // ?
        }

        // Test for (e1 ? e2 : e3).T contains intersect(e2.T, e3.T)
        if (Ktrue() ? o2 instanceof Integer x : o2 instanceof Integer x) {
            x.intValue();
        }

        // Test for (e1 ? e2 : e3).T contains intersect(e1.T, e3.T)
        if (o1 instanceof String s ? true : o1 instanceof String s) {
            s.length();
        }

        // Test for (e1 ? e2 : e3).T contains intersect(e1.F, e2.T)
        if (!(o1 instanceof String s) ? (o1 instanceof String s) : true) {
            s.length();
        }

        // Test for (e1 ? e2 : e3).F contains intersect(e2.F, e3.F)
        if (Ktrue() ? !(o2 instanceof Integer x) : !(o2 instanceof Integer x)){
        } else {
            x.intValue();
        }

        // Test for (e1 ? e2 : e3).F contains intersect(e1.T, e3.F)
        if (o1 instanceof String s ? true : !(o1 instanceof String s)){
        } else {
            s.length();
        }

        // Test for (e1 ? e2 : e3).F contains intersect(e1.F, e2.F)
        if (!(o1 instanceof String s) ? !(o1 instanceof String s) : true){
        } else {
            s.length();
        }

        L3: {
            if ((o1 instanceof String s) || (o3 instanceof String s)) {
                s.length();
            } else {
                break L3;
            }
            s.length();
        }

        System.out.println("BindingsTest1Merging complete");
    }
}
