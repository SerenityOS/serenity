/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6331574
 * @summary test isModifiableClass
 * @author Robert Field, Sun Microsystems
 *
 * @run build IsModifiableClassApp IsModifiableClassAgent
 * @run shell MakeJAR3.sh IsModifiableClassAgent 'Can-Retransform-Classes: true'
 * @run main/othervm -javaagent:IsModifiableClassAgent.jar IsModifiableClassApp
 */
import java.lang.instrument.*;

public class IsModifiableClassAgent
{
    public static boolean fail = false;
    public static boolean completed = false;

    public static void
    premain(    String agentArgs,
                Instrumentation instrumentation)
        {
            System.out.println("IsModifiableClassAgent started");

            Class[] allClasses = instrumentation.getAllLoadedClasses();
            int modCount = 0;
            int unmodCount = 0;

            for (int i = 0; i < allClasses.length; i++)
                {
                    Class klass = allClasses[i];
                    boolean isMod = instrumentation.isModifiableClass(klass);
                    if (isMod && klass.isArray()) {
                        System.err.println("Error: array class returned as modifiable: " + klass);
                        fail = true;
                    }
                    if (isMod && klass.isPrimitive()) {
                        System.err.println("Error: primitive class returned as modifiable: " + klass);
                        fail = true;
                    }
                    try {
                        instrumentation.retransformClasses(klass);
                        if (!isMod) {
                            System.err.println("Error: unmodifiable class retransformable: " + klass);
                            fail = true;
                        }
                    } catch (UnmodifiableClassException e) {
                        if (isMod) {
                            System.err.println("Error: modifiable class not retransformable: " + klass);
                            System.err.println("  exception: " + e);
                            fail = true;
                        }
                    } catch (Throwable e) {
                        System.err.println("Error: bad return from retransform: " + klass);
                        System.err.println("  ERROR: " + e);
                        fail = true;
                    }
                    if (isMod) {
                        ++modCount;
                    } else {
                        ++unmodCount;
                    }
                }
            System.out.println("modifiable: " + modCount + ". unmodifiable: " + unmodCount);
            completed = true;
        }
}
