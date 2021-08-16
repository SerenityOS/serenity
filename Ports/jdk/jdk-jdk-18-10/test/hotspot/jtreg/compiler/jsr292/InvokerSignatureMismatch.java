package compiler.jsr292;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandleHelper;
import jdk.internal.vm.annotation.ForceInline;

/*
 * @test
 * @bug 8166110
 * @library /test/lib / patches
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.vm.annotation
 *
 * @build java.base/java.lang.invoke.MethodHandleHelper
 * @run main/bootclasspath/othervm -XX:+IgnoreUnrecognizedVMOptions -Xbatch -XX:-TieredCompilation
 *                                 compiler.jsr292.InvokerSignatureMismatch
 */
public class InvokerSignatureMismatch {

    static final MethodHandle INT_MH;

    static {
        MethodHandle mhI = null;
        try {
           mhI = MethodHandles.lookup().findStatic(InvokerSignatureMismatch.class, "bodyI", MethodType.methodType(void.class, int.class));
        } catch (Throwable e) {
        }
        INT_MH = mhI;
    }

    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < 50_000; i++) { // OSR
            mainLink(i);
            mainInvoke(i);
        }
    }

    static void mainLink(int i) throws Throwable {
        Object name = MethodHandleHelper.internalMemberName(INT_MH);
        MethodHandleHelper.linkToStatic((float) i, name);
    }

    static void mainInvoke(int i) throws Throwable {
        MethodHandleHelper.invokeBasicV(INT_MH, (float) i);
    }

    static int cnt = 0;
    static void bodyI(int x) {
        if ((x & 1023) == 0) { // already optimized x % 1024 == 0
            ++cnt;
        }
    }

}
