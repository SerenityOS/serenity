/* /nodynamiccopyright/ hard coded linenumbers in other tests - DO NOT CHANGE
 * Debuggee which exercises various reference types
 */

abstract class AllAbstract {
    abstract void a();
    abstract void b();
    abstract void c();
}

class AllNative {
    native void a();
    native void b();
    native void c();
}

abstract class Abstract {
    abstract void a();
    void b() {
        int x = 1;
        int y = 2;
        System.out.println("x + y = " + x + y);
        return;
    }
}

class Native {
    native void a();
    void b() {
        int x = 1;
        int y = 2;
        System.out.println("x + y = " + x + y);
        return;
    }
}

abstract class AbstractAndNative {
    abstract void a();
    native void b();
    void c() {
        int x = 1;
        int y = 2;
        System.out.println("x + y = " + x + y);
        return;
    }
}

interface Interface {
    void a();
    void b();
    void c();
}

interface InterfaceWithCode {
    String foo = new String("foo");
}

public class RefTypes {
    static void loadClasses() throws ClassNotFoundException {
        Class.forName("AllAbstract");
        Class.forName("AllNative");
        Class.forName("Abstract");
        Class.forName("Native");
        Class.forName("AbstractAndNative");
        Class.forName("Interface");
        Class.forName("InterfaceWithCode");
    }

    public static void main(String args[]) throws Exception {
        loadClasses();
    }
}
