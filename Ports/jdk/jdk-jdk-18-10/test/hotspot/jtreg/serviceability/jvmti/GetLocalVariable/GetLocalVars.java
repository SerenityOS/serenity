/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8080406
 * @summary VM_GetOrSetLocal doesn't check local slot type against requested type
 * @requires vm.jvmti
 * @compile GetLocalVars.java
 * @run main/othervm/native -Xcomp -agentlib:GetLocalVars GetLocalVars
 */


public class GetLocalVars {
    private static final String agentLib = "GetLocalVars";

    static native void testLocals(Thread thread);
    static native int getStatus();

    public static
    void main(String[] args) throws Exception {
        try {
            System.loadLibrary(agentLib);
        } catch (UnsatisfiedLinkError ex) {
            System.err.println("Failed to load " + agentLib + " lib");
            System.err.println("java.library.path: " + System.getProperty("java.library.path"));
            throw ex;
        }
        run(args);
        int status = getStatus();
        if (status != 0) {
            throw new RuntimeException("Test GetLocalVars failed with a bad status: " + status);
        }
    }

    public static
    void run(String argv[]) {
        GetLocalVars testedObj = new GetLocalVars();
        double pi = 3.14d;
        byte sym = 'X';
        int year = 2018;

        staticMeth(sym, testedObj, pi, year);
    }

    public static synchronized
    int staticMeth(byte byteArg, Object objArg, double dblArg, int intArg) {
        testLocals(Thread.currentThread());
        {
            int intLoc = 9999;
            intArg = intLoc;
        }
        return intArg;
    }
}
