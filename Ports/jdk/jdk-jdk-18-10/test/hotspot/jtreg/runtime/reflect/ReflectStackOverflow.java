/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4185411
 * @summary This program crashes in 1.1, but runs okay in 1.2.
 * @run main/othervm -Xss512k ReflectStackOverflow
 */
import java.lang.reflect.*;

public class ReflectStackOverflow {
    private static final int COUNT = 11000;

    public static void main(String[] cmdline) throws Throwable {
        for (int i = 0; i < COUNT+1; i++) {
            stuff(i);
        }
    }

    private static void stuff(int count) throws Throwable {
        if (count < COUNT)
            return;  // don't do anything the first COUNT times.

        try {
            final Method method =
                Method.class.getMethod
                ("invoke", new Class[] { Object.class, Object[].class });

            final Object[] args = new Object[] { method, null };
            args[1] = args;

            method.invoke(method, args); // "recursive reflection"
            // exception should have been thrown by now...
            System.out.println("how did I get here?");
        } catch(Throwable t) {
            int layers;
            for(layers = 0; t instanceof InvocationTargetException; layers++)
                t = ((InvocationTargetException)t).getTargetException();

            System.err.println("Found " + layers + " layers of wrappers.");
            if (!(t instanceof StackOverflowError)) {
                throw t;
            }
        }
    }
}
