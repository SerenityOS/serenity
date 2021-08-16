/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4809008
 * @build Bean
 * @run main/othervm -server -XX:SoftRefLRUPolicyMSPerMB=0 Test4809008
 * @summary Tests memory leak with multiple class loader access
 * @author Mark Davidson
 */

import java.beans.BeanInfo;
import java.beans.Introspector;

/**
 * This tests to see if the classes will be garbage collected when multiple
 * short lived classloaders populate the BeanInfo cache with classes.
 * <p/>
 * We are also trying to verify if the ClassLoader finalize method is called.
 * <p/>
 * Use:
 * java -verbose:class      to print out class loading.
 * java -verbose:gc         to print out gc events.
 */
public class Test4809008 {
    public static void main(String[] args) throws Exception {
        printMemory("Start Memory");
        int introspected = 200;
        for (int i = 0; i < introspected; i++) {
            ClassLoader cl = new SimpleClassLoader();
            Class type = cl.loadClass("Bean");
            type.newInstance();

            // The methods and the bean info should be cached
            BeanInfo info = Introspector.getBeanInfo(type);

            cl = null;
            type = null;
            info = null;
            System.gc();
        }
        System.runFinalization();
        printMemory("End Memory");

        int finalized = SimpleClassLoader.numFinalizers;
        System.out.println(introspected + " classes introspected");
        System.out.println(finalized + " classes finalized");

        // good if at least half of the finalizers are run
        if (finalized < (introspected >> 1)) {
            throw new Error("ClassLoaders not finalized: " + finalized);
        }
    }

    private static void printMemory(String message) {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long free = runtime.freeMemory();
        long total = runtime.totalMemory();
        System.out.println(message);
        System.out.println("\tfree:  " + free);
        System.out.println("\ttotal: " + total);
    }
}
