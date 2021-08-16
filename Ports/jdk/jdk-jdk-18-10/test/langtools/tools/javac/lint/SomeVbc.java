/* /nodynamiccopyright/ */

package java.lang;

@jdk.internal.ValueBased
public final class SomeVbc {

    public SomeVbc() {}

    final String ref = "String";

    void abuseVbc() {

        synchronized(ref) {           // OK
            synchronized (this) {     // WARN
            }
        }
    }
}

final class AuxilliaryAbuseOfVbc {

    void abuseVbc(SomeVbc vbc) {

        synchronized(this) {           // OK
            synchronized (vbc) {       // WARN
            }
        }
    }
}

