/*
 * @test /nodynamiccopyright/
 * @bug 8029102
 * @summary Enhance compiler warnings for Lambda
 *     Checks that the warning for accessing non public members of a class is
 *     fired correctly.
 * @compile/fail/ref=WarnSerializableLambdaTestb.out -XDrawDiagnostics -Werror -XDwarnOnAccessToMembers WarnSerializableLambdaTestb.java
 */

import java.io.Serializable;

public class WarnSerializableLambdaTestb {
     public void foo(Secret1 secret) {
         Object o = (Runnable & java.io.Serializable) () -> { secret.test(); };
     }

     public void bar(Secret2 secret) {
         Object o = (Runnable & java.io.Serializable) () -> { secret.test(); };
     }

     private class Secret1 {
         public void test() {}
     }

     static private class Secret2 {
         public void test() {}
     }

     class TestInner {
        private int j = 0;
        void m() {
            Serializable s = new Serializable() {
                int i;
                void m() {
                    i = 0;  // don't warn
                    System.out.println(j); //warn
                }
            };
        }
    }

    class TestInner2 {
        class W implements Serializable {
            public int p = 0;
            class I {
                public int r = 0;
                class K implements Serializable {
                    void m() {
                        p = 1;  // don't warn owner is serializable
                        r = 2;  // warn owner is not serializable
                    }
                }
            }
        }
    }
}
