/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test WhiteBox
 * @bug 8011675
 * @summary verify that whitebox can be used even if not all functions are declared in java-part
 * @author igor.ignatyev@oracle.com
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile WhiteBox.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:-CheckIntrinsics jdk.test.whitebox.WhiteBox
 */

package jdk.test.whitebox;

public class WhiteBox {
    @SuppressWarnings("serial")
    public static class WhiteBoxPermission extends java.security.BasicPermission {
        // ClassFileInstaller is hard-coded to copy WhiteBox$WhiteBoxPermission, so let's
        // make a fake one here as well.
        public WhiteBoxPermission(String s) {
            super(s);
        }
    }

    private static native void registerNatives();
    static { registerNatives(); }
    public native int notExistedMethod();
    public native int getHeapOopSize();
    public static void main(String[] args) {
        WhiteBox wb = new WhiteBox();
        if (wb.getHeapOopSize() < 0) {
            throw new Error("wb.getHeapOopSize() < 0");
        }
        boolean catched = false;
        try {
            wb.notExistedMethod();
        } catch (UnsatisfiedLinkError e) {
            catched = true;
        }
        if (!catched) {
            throw new Error("wb.notExistedMethod() was invoked");
        }
    }
}
