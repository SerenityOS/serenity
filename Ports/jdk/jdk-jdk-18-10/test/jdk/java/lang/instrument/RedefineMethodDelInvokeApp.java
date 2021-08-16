/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.lang.instrument.*;

public class RedefineMethodDelInvokeApp {
    public static void main(String args[]) {
        System.out.println("Hello from RedefineMethodDelInvokeApp!");

        try {
            new RedefineMethodDelInvokeApp().doTest();
        } catch (Exception ex) {
            System.out.println("Exception has been caught");
            ex.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private void doTest() throws Exception {
        RedefineMethodDelInvokeTarget target =
            new RedefineMethodDelInvokeTarget();

        System.out.println("RedefineMethodDelInvokeApp: invoking myMethod0(), myMethod1(), myMethod2()");
        target.test();

        // delete myMethod2()
        do_redefine(1);

        System.out.println("RedefineMethodDelInvokeApp: invoking myMethod0(), myMethod1()");
        target.test();

        // delete myMethod1()
        do_redefine(2);

        System.out.println("RedefineMethodDelInvokeApp: invoking myMethod0()");
        target.test();
    }

    private static void do_redefine(int counter) throws Exception {
        File f = new File("RedefineMethodDelInvokeTarget_" + counter +
            ".class");
        System.out.println("Reading test class from " + f);
        InputStream redefineStream = new FileInputStream(f);

        byte[] redefineBuffer = NamedBuffer.loadBufferFromStream(redefineStream);

        ClassDefinition redefineParamBlock = new ClassDefinition(
            RedefineMethodDelInvokeTarget.class, redefineBuffer);

        RedefineMethodDelInvokeAgent.getInstrumentation().redefineClasses(
            new ClassDefinition[] {redefineParamBlock});
    }
}
