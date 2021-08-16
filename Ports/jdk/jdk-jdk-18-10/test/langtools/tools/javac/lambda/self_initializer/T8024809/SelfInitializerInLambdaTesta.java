/*
 * @test /nodynamiccopyright/
 * @bug 8024809
 * @summary javac, some lambda programs are rejected by flow analysis
 * @compile/fail/ref=SelfInitializerInLambdaTesta.out -XDrawDiagnostics SelfInitializerInLambdaTesta.java
 */

public class SelfInitializerInLambdaTesta {

    final Runnable r1 = ()->System.out.println(r1);

    final Object lock = new Object();

    final Runnable r2 = ()->{
        System.out.println(r2);
        synchronized (lock){}
    };

    final Runnable r3 = ()->{
        synchronized (lock){
            System.out.println(r3);
        }
    };

    final Runnable r4 = ()->{
        System.out.println(r4);
    };

    interface SAM {
        int m(String s);
    }

    final SAM s1 = (String s)->{
        System.out.println(s + s1.toString());
        return 0;
    };

    final SAM s2 = (s)->{
        System.out.println(s + s2.toString());
        return 0;
    };
}
