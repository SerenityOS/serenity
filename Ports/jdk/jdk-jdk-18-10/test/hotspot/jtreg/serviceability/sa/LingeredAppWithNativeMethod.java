
/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Random;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.Utils;

public class LingeredAppWithNativeMethod extends LingeredApp {

    public static final String THREAD_NAME = "NoFramePointerJNIFib";
    private static final int UPPER_BOUND = 55;
    private static final int LOWER_BOUND = 40;
    private static final Random RNG = Utils.getRandomInstance();

    static {
        // JNI library compiled with no frame pointer info
        System.loadLibrary("NoFramePointer");
    }

    public void callNative() {
        // Call JNI code which does something compute
        // intensive: fibonacci
        // That is to ensure that the native bits run when
        // jstack --mixed info is to be gathered.
        // Results of fibonacci calculation from JNI are
        // reported via callback(). That's where the process
        // of calculating fibonacci restarts.
        int num = (int) (RNG.nextDouble() * UPPER_BOUND);
        while (num < LOWER_BOUND) {
            num = (int) (RNG.nextDouble() * UPPER_BOUND);
        }
        System.out.print("fib(" + num + ") = ");
        callJNI(this, num);
    }

    // Called from JNI library libNoFramePointer
    private void callback(long val) {
        System.out.println(val);
        // Call native again so as to increase chances of
        // being currently in JNI code when jstack --mixed
        // runs.
        callNative();
    }

    public static native void callJNI(Object target, int num);

    public static void main(String[] args) {
        LingeredAppWithNativeMethod app = new LingeredAppWithNativeMethod();
        Thread fibonacci = new Thread(() -> {
            app.callNative();
        });
        fibonacci.setName(THREAD_NAME);
        fibonacci.start();
        LingeredApp.main(args);
    }
}
