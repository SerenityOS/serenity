/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8166188
 * @summary Test call of native function with JNI weak global ref.
 * @run main/othervm/native CallWithJNIWeak
 */

public class CallWithJNIWeak {
    static {
        System.loadLibrary("CallWithJNIWeak");
    }

    private static native void testJNIFieldAccessors(CallWithJNIWeak o);

    // The field initializations must be kept in sync with the JNI code
    // which reads verifies the values of these fields.
    private int i = 1;
    private long j = 2;
    private boolean z = true;
    private char c = 'a';
    private short s = 3;
    private float f = 1.0f;
    private double d = 2.0;
    private Object l;

    private CallWithJNIWeak() {
        this.l = this;
    }

    private native void weakReceiverTest0();
    private void weakReceiverTest() {
        weakReceiverTest0();
    }

    private synchronized void synchonizedWeakReceiverTest() {
        this.notifyAll();
    }


    private static native void runTests(CallWithJNIWeak o);

    public static void main(String[] args) {
        CallWithJNIWeak w = new CallWithJNIWeak();
        for (int i = 0; i < 20000; i++) {
            runTests(w);
        }
    }
}
