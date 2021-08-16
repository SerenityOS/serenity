/*
 * @test /nodynamiccopyright/
 * @bug 8161985
 * @summary Spurious override of Object.getClass leads to NPE
 * @compile/fail/ref=T8161985a.out -XDrawDiagnostics T8161985a.java
 */

class T8161985 {
    public static void main(String [] arg) {
        T8161985 t = new T8161985();
        t.getClass();

    }
    public void getClass() {
        Fred1 f = new Fred1();
        System.out.println( "fred classname: " + f.getClassName());
    }


    abstract class Fred {
        public String getClassName() {
            return this.getClass().getSimpleName();
        }
    }

    class Fred1 extends Fred {
    }
}
