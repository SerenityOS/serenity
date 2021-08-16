/*
 * @test /nodynamiccopyright/
 * @bug 8231827
 * @summary Ensure that scopes arising from conditionalExpressions are handled corrected.
 * @compile/fail/ref=BindingsTest2.out -XDrawDiagnostics -XDshould-stop.at=FLOW BindingsTest2.java
 */
public class BindingsTest2 {
    public static boolean Ktrue() { return true; }
    public static void main(String[] args) {
        Object o1 = "hello";
        Integer in = 42;
        Object o2 = in;
        Object o3 = "there";


        if (Ktrue() ? o2 instanceof Integer x : o2 instanceof String x) {
            x.intValue();
        }
        if (Ktrue() ? o2 instanceof Integer x : true) {
            x.intValue();
        }

        if (o1 instanceof String s ? true : true) {
            s.length();
        }
        if (o1 instanceof String s ? true : o2 instanceof Integer s) {
            s.length();
        }
        if (o1 instanceof String s ? true : o2 instanceof Integer i) {
            s.length();
        }

        // Test for (e1 ? e2 : e3).T contains intersect(e1.F, e2.T)
        if (!(o1 instanceof String s) ? true : true) {
            s.length();
        }
        if (!(o1 instanceof String s) ? (o2 instanceof Integer s) : true) {
            s.length();
        }
        if (!(o1 instanceof String s) ? (o2 instanceof Integer i) : true) {
            s.length();
            i.intValue();
        }
        if (!(o1 instanceof String s) ? (o1 instanceof String s2) : true) {
            s.length();
            s2.length();
        }


        // Test for (e1 ? e2 : e3).F contains intersect(e2.F, e3.F)
        if (Ktrue() ? !(o2 instanceof Integer x) : !(o1 instanceof String x)){
        } else {
            x.intValue();
        }
        if (Ktrue() ? !(o2 instanceof Integer x) : !(o1 instanceof String s)){
        } else {
            x.intValue();
        }
        if (Ktrue() ? !(o2 instanceof Integer x) : !(o2 instanceof Integer x1)){
        } else {
            x.intValue();
            x1.intValue();
        }
        if (Ktrue() ? !(o2 instanceof Integer x) : false){
        } else {
            x.intValue();
        }

        // Test for (e1 ? e2 : e3).F contains intersect(e1.T, e3.F)
        if (o1 instanceof String s ? true : !(o2 instanceof Integer s)){
        } else {
            s.length();
        }
        if (o1 instanceof String s ? true : !(o2 instanceof Integer i)){
        } else {
            s.length();
            i.intValue();
        }
        if (o1 instanceof String s ? true : !(o2 instanceof String s1)){
        } else {
            s.length();
            s1.length();
        }
        // Test for (e1 ? e2 : e3).F contains intersect(e1.F, e2.F)
        if (!(o1 instanceof String s) ? !(o1 instanceof String s1) : true){
        } else {
            s.length();
            s1.length();
        }
        if (!(o1 instanceof String s) ? !(o2 instanceof Integer s) : true){
        } else {
            s.length();
        }
        if (!(o1 instanceof String s) ? !(o2 instanceof Integer i) : true){
        } else {
            s.length();
            i.intValue();
        }

        // Test for e1 ? e2: e3 - include e1.T in e2
        if (o1 instanceof String s ? false : s.length()>0) {
            System.out.println("done");
        }
        if (o1 instanceof String s ? false : s.intValue!=0) {
            System.out.println("done");
        }

        // Test for e1 ? e2 : e3 - include e1.F in e3
        if (!(o1 instanceof String s) ? s.length()>0 : false){
            System.out.println("done");
        }
        if (!(o1 instanceof String s) ? s.intValue>0 : false){
            System.out.println("done");
        }

        {
            while (!(o1 instanceof String s)) {
                break;
            }

            s.length();
        }

        {
            while (!(o1 instanceof String s)) {
                if (false) break;
            }

            s.length();
        }

        {
            while (!(o1 instanceof String s)) {
                while (true);
                break;
            }

            s.length();
        }

        {
            for (; !(o1 instanceof String s); ) {
                break;
            }

            s.length();
        }

        {
            for (; !(o1 instanceof String s); ) {
                if (false) break;
            }

            s.length();
        }

        {
            for (; !(o1 instanceof String s); ) {
                while (true);
                break;
            }

            s.length();
        }

        {
            do {
                break;
            } while (!(o1 instanceof String s));

            s.length();
        }

        {
            do {
                if (false) break;
            } while (!(o1 instanceof String s));

            s.length();
        }

        {
            do {
                while (true);
                break;
            } while (!(o1 instanceof String s));

            s.length();
        }

        {
            L: while (!(o1 instanceof String s)) {
                break L;
            }

            s.length();
        }

        {
            L: for (; !(o1 instanceof String s); ) {
                break L;
            }

            s.length();
        }

        {
            L: do {
                break L;
            } while (!(o1 instanceof String s));

            s.length();
        }

        {
            L: {
                while (!(o1 instanceof String s)) {
                    break L;
                }

                s.length();
            }
        }

        {
            L: {
                for (; !(o1 instanceof String s); ) {
                    break L;
                }

                s.length();
            }
        }

        {
            L: {
                do {
                    break L;
                } while (!(o1 instanceof String s));

                s.length();
            }
        }

        {
            if (o1 instanceof final String s) {
                s = "";
            }
        }
    }
}
