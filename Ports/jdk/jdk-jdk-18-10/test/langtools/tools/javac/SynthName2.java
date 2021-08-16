/*
 * @test /nodynamiccopyright/
 * @bug 4462714
 * @summary using of synthetic names in local class causes ClassFormatError
 * @author gafter
 *
 * @compile/fail/ref=SynthName2.out -XDrawDiagnostics SynthName2.java
 */

import java.io.PrintStream;

class SynthName2 {
    public static void main(String args[]) {
        run(args, System.out);
    }
    public static void run(String args[],PrintStream out) {
        int  res1, res2;
        Intf ob = meth(1, 2);

        res1 = ob.getFirst();
        res2 = ob.getSecond();

        if ( res1 == 1 && res2 == 2 )
            return;
        out.println("Failed:  res1=" + res1 + ", res2=" + res2);
        throw new Error("test failed!");
    }
    interface Intf {
        int getFirst();
        int getSecond();
    }
    static Intf meth(final int prm1, final int zzz) {
        class InnClass implements Intf {
            int val$prm1 = prm1;
            int val$zzz  = zzz;
            int locVar;
            public int getFirst() {
                locVar = val$prm1;
                return prm1;
            }
            public int getSecond() {
                return zzz;
            }
        }
        return new InnClass();
    }
}
