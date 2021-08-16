/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;

import java.lang.management.ManagementFactory;
import java.util.Objects;

public class VMRuntimeEnvUtils {
    private static HotSpotDiagnosticMXBean DIAGNOSTIC_BEAN
            = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);

    private VMRuntimeEnvUtils() {
    }

    public static boolean isJITEnabled() {
        boolean isJITEnabled = ManagementFactory.getCompilationMXBean() != null;

        return isJITEnabled;
    }

    /**
     * Returns value of VM option.
     *
     * @param name option's name
     * @return value of option or {@code null}, if option doesn't exist
     * @throws NullPointerException if name is null
     * @see HotSpotDiagnosticMXBean#getVMOption(String)
     */
    public static String getVMOption(String name) {
        Objects.requireNonNull(name);
        VMOption tmp;
        try {
            tmp = DIAGNOSTIC_BEAN.getVMOption(name);
        } catch (IllegalArgumentException e) {
            tmp = null;
        }
        return (tmp == null ? null : tmp.getValue());
    }

    /**
     * Returns value of VM option or default value.
     *
     * @param name         option's name
     * @param defaultValue default value
     * @return value of option or {@code defaultValue}, if option doesn't exist
     * @throws NullPointerException if name is null
     * @see #getVMOption(String)
     */
    public static String getVMOption(String name, String defaultValue) {
        String result = getVMOption(name);
        return result == null ? defaultValue : result;
    }

    /**
     * Returns if a boolean VM option is enabled or not.
     *
     * @param name  option's name
     * @return true iff enabled
     * @throws IllegalArgumentException if naming non-boolean or non-existing option
     */
    public static boolean isVMOptionEnabled(String name) {
        String isSet = getVMOption(name, "error");
        if (isSet.equals("true")) {
            return true;
        } else if (isSet.equals("false")) {
            return false;
        }
        throw new IllegalArgumentException(name + " is not a boolean option.");
    }

    /**
     * Sets a specified value for VM option of given name.
     *
     * @param name  option's name
     * @param value new value
     * @throws NullPointerException     if name is null
     * @throws IllegalArgumentException if new value is invalid or if vm option
     *                                  is not writable.
     * @see HotSpotDiagnosticMXBean#setVMOption(String, String)
     */
    public static void setVMOption(String name, String value) {
        Objects.requireNonNull(name);
        DIAGNOSTIC_BEAN.setVMOption(name, value);
    }
}
