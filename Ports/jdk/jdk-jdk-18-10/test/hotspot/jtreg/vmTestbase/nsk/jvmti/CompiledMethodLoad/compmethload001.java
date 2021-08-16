/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.CompiledMethodLoad;

import java.io.*;

import nsk.share.*;

/**
 * This test exercises the JVMTI event <code>CompiledMethodLoad</code>.
 * <br>It creates an instance of tested class 'HotClass'. Then special
 * method 'hotMethod' is called in a loop in order to be compiled/inlined.
 * The CompiledMethodLoad events, if they occur, must be sent only
 * during the live phase of the VM execution.
 */
public class compmethload001 {
    /* number of interations to provoke method compiling */
    final static int ITERATIONS = 10000;

    static {
        try {
            System.loadLibrary("compmethload001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"compmethload001\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native int check();

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new compmethload001().runThis(argv, out);
    }

    private int runThis(String argv[], PrintStream out) {
        HotClass hotCls = new HotClass(ITERATIONS);

        hotCls.entryMethod(); /* provoke method compiling */

        return check();
    }

    /**
     * Dummy class used only to provoke method compiling/inlining
     * and, thus, CompiledMethodLoad event generation.
     */
    class HotClass {
        StringBuffer strBuf;
        int iter;

        HotClass(int iter) {
            strBuf = new StringBuffer(iter);
            this.iter = iter;
        }

        void entryMethod() {
            for (int i=0; i<iter; i++)
                hotMethod(i);
        }

        void hotMethod(int i) {
            strBuf.append(Integer.toString(i) + " ");
        }
    }
}
