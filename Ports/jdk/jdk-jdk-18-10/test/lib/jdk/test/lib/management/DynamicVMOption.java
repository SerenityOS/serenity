/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.management;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;

/**
 * A utility class to work with VM options which could be altered during
 * execution.
 *
 * This class is a wrapper around {@code com.sun.management.VMOption}.
 * It provides more convenient interface to read/write the values.
 *
 */
public class DynamicVMOption {

    private final HotSpotDiagnosticMXBean mxBean;

    /**
     * VM option name, like "MinHeapFreeRatio".
     */
    public final String name;

    /**
     * Creates an instance of DynamicVMOption.
     *
     * @param name the VM option name
     */
    public DynamicVMOption(String name) {
        this.name = name;
        mxBean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
    }

    /**
     * Sets a new value for the option.
     * Trying to set not applicable value will cause IllegalArgumentException.
     * Behavior with null is undefined, most likely NPE will be thrown.
     *
     * @param newValue the value to be set
     * @see #getValue()
     * @throws IllegalArgumentException if newValue is not applicable to the option
     */
    public final void setValue(String newValue) {
        mxBean.setVMOption(name, newValue);
    }

    /**
     * Returns the value of option.
     *
     * @return the current option value
     * @see #setValue(java.lang.String)
     */
    public final String getValue() {
        return mxBean.getVMOption(name).getValue();
    }

    /**
     * Returns true, if option is writable, false otherwise.
     *
     * @return true, if option is writable, false otherwise
     */
    public final boolean isWriteable() {
        return mxBean.getVMOption(name).isWriteable();
    }

    /**
     * Checks if the given value is applicable for the option.
     *
     * This method tries to set the option to the new value. If no exception
     * has been thrown the value is treated as valid.
     *
     * Calling this method will not change the option value. After an attempt
     * to set a new value, the option will be restored to its previous value.
     *
     * @param value the value to verify
     * @return true if option could be set to the given value
     */
    public boolean isValidValue(String value) {
        boolean isValid = true;
        String oldValue = getValue();
        try {
            setValue(value);
        } catch (NullPointerException e) {
            if (value == null) {
                isValid = false;
            }
        } catch (IllegalArgumentException e) {
            isValid = false;
        } finally {
            setValue(oldValue);
        }
        return isValid;
    }

    /**
     * Returns the value of the given VM option as String.
     *
     * This is a simple shortcut for {@code new DynamicVMOption(name).getValue()}
     *
     * @param name the name of VM option
     * @return value as a string
     * @see #getValue()
     */
    public static String getString(String name) {
        return new DynamicVMOption(name).getValue();
    }

    /**
     * Returns the value of the given option as int.
     *
     * @param name the name of VM option
     * @return value parsed as integer
     * @see #getString(java.lang.String)
     *
     */
    public static int getInt(String name) {
        return Integer.parseInt(getString(name));
    }

    /**
     * Sets the VM option to a new value.
     *
     * This is a simple shortcut for {@code new DynamicVMOption(name).setValue(value)}
     *
     * @param name the name of VM option
     * @param value the value to be set
     * @see #setValue(java.lang.String)
     */
    public static void setString(String name, String value) {
        new DynamicVMOption(name).setValue(value);
    }

    /**
     * Sets the VM option value to a new integer value.
     *
     * @param name the name of VM option
     * @param value the integer value to be set
     * @see #setString(java.lang.String, java.lang.String)
     */
    public static void setInt(String name, int value) {
        new DynamicVMOption(name).setValue(Integer.toString(value));
    }

}
