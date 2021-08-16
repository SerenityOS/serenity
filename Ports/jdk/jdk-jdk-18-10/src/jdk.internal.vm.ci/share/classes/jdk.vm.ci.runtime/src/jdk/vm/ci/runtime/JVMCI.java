/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.runtime;

import java.util.Formatter;

import jdk.vm.ci.common.NativeImageReinitialize;
import jdk.vm.ci.services.Services;

public class JVMCI {

    /**
     * Singleton instance lazily initialized via double-checked locking.
     */
    @NativeImageReinitialize private static volatile JVMCIRuntime runtime;

    @NativeImageReinitialize private static boolean initializing;

    public static void initialize() {
        // force static initializer
    }

    private static native JVMCIRuntime initializeRuntime();

    /**
     * Gets the singleton {@link JVMCIRuntime} instance available to the application.
     *
     * @throws UnsupportedOperationException if JVMCI is not supported
     */
    public static JVMCIRuntime getRuntime() {
        JVMCIRuntime result = runtime;
        if (result == null) {
            synchronized (JVMCI.class) {
                result = runtime;
                if (result == null) {
                    if (initializing) {
                        // In recursive call from HotSpotJVMCIRuntime.runtime
                        // so no need to re-enter initializeRuntime below. This
                        // path is only entered if JVMCI initialization starts
                        // with JVMCI.getRuntime().
                        return null;
                    }
                    initializing = true;
                    try {
                        runtime = result = initializeRuntime();
                    } catch (UnsatisfiedLinkError e) {
                        String javaHome = Services.getSavedProperty("java.home");
                        String vmName = Services.getSavedProperty("java.vm.name");
                        Formatter errorMessage = new Formatter();
                        errorMessage.format("The VM does not support the JVMCI API.%n");
                        errorMessage.format("Currently used Java home directory is %s.%n", javaHome);
                        errorMessage.format("Currently used VM configuration is: %s", vmName);
                        throw new UnsupportedOperationException(errorMessage.toString());
                    } finally {
                        initializing = false;
                    }
                }
            }
        }
        return result;
    }
}
