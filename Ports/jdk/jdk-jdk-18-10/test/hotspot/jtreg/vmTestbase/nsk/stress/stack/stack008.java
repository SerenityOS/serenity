/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @key stress
 *
 * @summary converted from VM testbase nsk/stress/stack/stack008.
 * VM testbase keywords: [stress, stack, nonconcurrent]
 * VM testbase readme:
 * DESCRIPTION
 *     This test provokes multiple stack overflows in the same thread
 *     by invocations via reflection. Recursive method is invoked for
 *     the given fixed depth of recursion (though, for a large depth).
 *     This test makes measures a number of recursive invocations
 *     before 1st StackOverflowError, and then tries to reproduce
 *     such StackOverflowError 100 times -- each time by trying to
 *     invoke the same recursive method for the given fixed depth
 *     of invocations (which is twice that depth just measured).
 *     The test is deemed passed, if VM have not crashed.
 * COMMENTS
 *     This test crashes all HS versions (2.0, 1.3, 1.4) on Solaris,
 *     and crashes HS 2.0 on win32. However, it passes against HS 1.3
 *     and 1.4 on Win32.
 *     See the bug:
 *     4366625 (P4/S4) multiple stack overflow causes HS crash
 *     The stack size is too small to run on systems with > 4K page size.
 *     Making it bigger could cause timeouts on other platform.
 *
 * @requires (vm.opt.DeoptimizeALot != true & vm.compMode != "Xcomp" & vm.pageSize == 4096)
 * @run main/othervm/timeout=900 -Xss200K nsk.stress.stack.stack008
 */

package nsk.stress.stack;


import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class stack008 {
    public static void main(String[] args) {
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
    }

    public static int run(String args[], PrintStream out) {
        int depth;
        //
        // Measure maximal recursion depth until stack overflow:
        //
        for (depth = 100; ; depth += 100)
            try {
                invokeRecurse(depth);
            } catch (Throwable exception) {
                Throwable target = getTargetException(exception);
                if ((target instanceof StackOverflowError) ||
                        (target instanceof OutOfMemoryError))
                    break; // OK.
                target.printStackTrace(out);
                if (target instanceof ThreadDeath)
                    throw (ThreadDeath) target;
                return 2;
            }
        out.println("Max. depth: " + depth);
        //
        // Provoke stack overflow multiple times:
        //
        for (int i = 0; i < 100; i++)
            try {
                invokeRecurse(2 * depth);
//              out.println("?");
            } catch (Throwable exception) {
                Throwable target = getTargetException(exception);
                if ((target instanceof StackOverflowError) ||
                        (target instanceof OutOfMemoryError))
                    continue; // OK.
                target.printStackTrace(out);
                if (target instanceof ThreadDeath)
                    throw (ThreadDeath) target;
                return 2;
            }
        return 0;
    }

    private static Throwable getTargetException(Throwable exception) {
        Throwable target;
        //
        // Unwrap deep chain of exceptions:
        //
        for (
                target = exception;
                target instanceof InvocationTargetException;
                target = ((InvocationTargetException) target).getTargetException()
                )
            ;
        return target;
    }

    static Method method = null;
    static stack008 instance = null;
    static Object params[] = null;

    private static void invokeRecurse(int depth) throws Exception {
        if (method == null) {
            //
            // Optimization trick: allocate once, use everywhere.
            //
            instance = new stack008();
            method = stack008.class.getMethod("recurse");
            params = new Object[]{};
        }
        //
        // Note, that the same instance.depth is used in all invocations:
        //
        instance.depth = depth;
        method.invoke(instance, params);
    }

    int depth = 0;

    public void recurse() throws Exception {
        if (depth > 0)
            //
            // Self-invoke via reflection:
            //
            invokeRecurse(depth - 1);
    }
}
