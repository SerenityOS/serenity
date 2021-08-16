/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.runtime;

import sun.hotspot.WhiteBox;
import java.util.Objects;
import jdk.test.lib.Platform;

/**
 * @test
 * @bug 8153324
 * @summary Verify instrumented Throwable bytecode by compiling it with C1.
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.compMode!="Xint"
 * @build  sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -Xbatch -XX:StartFlightRecording:dumponexit=true jdk.jfr.event.runtime.TestThrowableInstrumentation
 */
public class TestThrowableInstrumentation {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static int COMP_LEVEL_SIMPLE = 1;

    private static boolean isTieredCompilationEnabled() {
        return Boolean.valueOf(Objects.toString(WHITE_BOX.getVMFlag("TieredCompilation")));
    }

    public static void main(String[] args) {
        // Compile Throwable::<clinit> with C1 (if available)
        if (!WHITE_BOX.enqueueInitializerForCompilation(java.lang.Throwable.class, COMP_LEVEL_SIMPLE)) {
          if (!Platform.isServer() || isTieredCompilationEnabled() || Platform.isEmulatedClient()) {
            throw new RuntimeException("Unable to compile Throwable::<clinit> with C1");
          }
        }
    }
}
