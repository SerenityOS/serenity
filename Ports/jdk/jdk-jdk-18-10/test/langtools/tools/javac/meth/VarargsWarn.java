/*
 * @test /nodynamiccopyright/
 * @bug 8019340
 * @summary varargs-related warnings are meaningless on signature-polymorphic methods such as MethodHandle.invokeExact
 *
 * @compile/fail/ref=VarargsWarn.out -XDrawDiagnostics -Werror VarargsWarn.java
 */

import java.lang.invoke.*;

class VarargsWarn {
    void test(MethodHandle mh) throws Throwable {
        mh.invokeExact((Integer[])null);
        mh.invoke((Integer[])null);
        mh.invokeWithArguments((Integer[])null); //not a sig poly method - warning here!
    }
}
