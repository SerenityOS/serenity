/*
 * Copyright (c) 2019 SAP SE and/or its affiliates. All rights reserved.
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
 * @bug 8227680
 * @summary Tests that all FieldAccess notifications for Get*Field
            with primitive type are generated.
 * @requires vm.jvmti
 * @compile FastGetField.java
 * @run main/othervm/native -agentlib:FastGetField -XX:+IgnoreUnrecognizedVMOptions -XX:-VerifyJNIFields FastGetField
 * @run main/othervm/native -agentlib:FastGetField -XX:+IgnoreUnrecognizedVMOptions -XX:-VerifyJNIFields -XX:+UnlockDiagnosticVMOptions -XX:+ForceUnreachable -XX:+SafepointALot -XX:GuaranteedSafepointInterval=1 FastGetField
 */

import java.lang.reflect.Field;


public class FastGetField {

    private static final String agentLib = "FastGetField";

    private native boolean initFieldIDs(Class c);
    private native boolean initWatchers(Class c);
    public native long accessFields(MyItem i);
    public static native long getFieldAccessCount();

    static final int loop_cnt = 10000;


    class MyItem {
        // Names should match JNI types.
        boolean Z;
        byte B;
        short S;
        char C;
        int I;
        long J;
        float F;
        double D;

        public void change_values() {
            Z = true;
            B = 1;
            C = 1;
            S = 1;
            I = 1;
            J = 1l;
            F = 1.0f;
            D = 1.0;
        }

        public void reset_values() {
            Z = false;
            B = 0;
            C = 0;
            S = 0;
            I = 0;
            J = 0l;
            F = 0.0f;
            D = 0.0;
        }
    }

    // Static initialization.
    static {
        try {
            System.loadLibrary(agentLib);
        } catch (UnsatisfiedLinkError ex) {
            System.err.println("Failed to load " + agentLib + " lib");
            System.err.println("java.library.path: " + System.getProperty("java.library.path"));
            throw ex;
        }
    }

    public void TestFieldAccess() throws Exception {
        MyItem i = new MyItem();
        if (!initFieldIDs(MyItem.class)) throw new RuntimeException("FieldID initialization failed!");

        long duration = System.nanoTime();
        for (int c = 0; c < loop_cnt; ++c) {
            if (accessFields(i) != 0l) throw new RuntimeException("Wrong initial result!");
            i.change_values();
            if (accessFields(i) != 8l) throw new RuntimeException("Wrong result after changing!");
            i.reset_values();
        }
        duration = System.nanoTime() - duration;
        System.out.println(loop_cnt + " iterations took " + duration + "ns.");

        if (getFieldAccessCount() != 0) throw new RuntimeException("Watch not yet active!");

        // Install watchers.
        if (!initWatchers(MyItem.class)) throw new RuntimeException("JVMTI missing!");

        // Try again with watchers.
        if (accessFields(i) != 0l) throw new RuntimeException("Wrong initial result!");
        i.change_values();
        if (accessFields(i) != 8l) throw new RuntimeException("Wrong result after changing!");
        if (getFieldAccessCount() != 16) throw new RuntimeException("Unexpected event count!");
    }

    public static void main(String[] args) throws Exception {
        FastGetField inst = new FastGetField();
        inst.TestFieldAccess();
    }
}
