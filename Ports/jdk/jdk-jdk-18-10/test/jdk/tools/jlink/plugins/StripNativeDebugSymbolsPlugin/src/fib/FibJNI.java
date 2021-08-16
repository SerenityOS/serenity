/*
 * Copyright (c) 2019, Red Hat, Inc
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
package fib;
public class FibJNI {

    static {
        // Native lib used for debug symbols stripping
        System.loadLibrary("Fib");
    }

    private final int num;
    private final long expected;

    public FibJNI(int num, long expected) {
        this.num = num;
        this.expected = expected;
    }

    public void callNative() {
        callJNI(this, num);
    }

    // Called from JNI library libFib
    private void callback(long val) {
        System.out.println("Debug: result was: " + val);
        if (val != expected) {
            throw new RuntimeException("Expected " + expected + " but got: " +val);
        }
    }

    public static native void callJNI(Object target, int num);

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: " + FibJNI.class.getName() + " <input> <expectedResult>");
        }
        int input = Integer.parseInt(args[0]);
        long expected = Long.parseLong(args[1]);
        FibJNI fib = new FibJNI(input, expected);
        fib.callNative();
        System.out.println("DEBUG: Sanity check for " + FibJNI.class.getSimpleName() + " passed.");
    }
}
