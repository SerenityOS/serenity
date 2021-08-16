/*
 * @test /nodynamiccopyright/
 * @bug 8254274
 * @summary lint should warn when an instance of a value based class is synchronized upon
 * @compile/fail/ref=JdkInternalAbuseOfVbc.out --patch-module java.base=${test.src} -XDrawDiagnostics -Werror -Xlint SomeVbc.java JdkInternalAbuseOfVbc.java
 */

package java.lang;

public final class JdkInternalAbuseOfVbc {

    public JdkInternalAbuseOfVbc() {}

    void abuseVbc(SomeVbc vbc) {

        synchronized(this) {           // OK
            synchronized (vbc) {       // WARN
            }
        }
    }
}

