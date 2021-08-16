package test.java.lang.invoke.AccessControlTest_subpkg;
import test.java.lang.invoke.AccessControlTest;
import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;

// This guy tests access from outside the package test.java.lang.invoke:
public class Acquaintance_remote {
    public static Lookup[] lookups() {
        return new Lookup[] {
            Acquaintance_remote.lookup_in_remote(),
            Remote_subclass.lookup_in_subclass(),
            Remote_hidden.lookup_in_hidden()
        };
    }

    public static Lookup lookup_in_remote() {
        return MethodHandles.lookup();
    }
    public static      void pub_in_remote() { }
    protected static   void pro_in_remote() { }
    static /*package*/ void pkg_in_remote() { }
    private static     void pri_in_remote() { }

    public static class Remote_subclass extends AccessControlTest {
        static Lookup lookup_in_subclass() {
            return MethodHandles.lookup();
        }
        public static      void pub_in_subclass() { }
        protected static   void pro_in_subclass() { }
        static /*package*/ void pkg_in_subclass() { }
        private static     void pri_in_subclass() { }
    }
    static /*package*/ class Remote_hidden {
        static Lookup lookup_in_hidden() {
            return MethodHandles.lookup();
        }
        public static      void pub_in_hidden() { }
        protected static   void pro_in_hidden() { }
        static /*package*/ void pkg_in_hidden() { }
        private static     void pri_in_hidden() { }
    }
}
