/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jpackage.internal;

import java.util.ArrayList;
import java.util.List;

final class WindowsRegistry {

    // Currently we only support HKEY_LOCAL_MACHINE. Native implementation will
    // require support for additinal HKEY if needed.
    private static final int HKEY_LOCAL_MACHINE = 1;

    static {
        System.loadLibrary("jpackage");
    }

    private WindowsRegistry() {}

    /**
     * Reads the registry value for DisableRealtimeMonitoring.
     * @return true if DisableRealtimeMonitoring is set to 0x1,
     *         false otherwise.
     */
    static final boolean readDisableRealtimeMonitoring() {
        final String subKey = "Software\\Microsoft\\"
                  + "Windows Defender\\Real-Time Protection";
        final String value = "DisableRealtimeMonitoring";
        int result = readDwordValue(HKEY_LOCAL_MACHINE, subKey, value, 0);
        return (result == 1);
    }

    static final List<String> readExclusionsPaths() {
        List<String> result = new ArrayList<>();
        final String subKey = "Software\\Microsoft\\"
                + "Windows Defender\\Exclusions\\Paths";
        long lKey = openRegistryKey(HKEY_LOCAL_MACHINE, subKey);
        if (lKey == 0) {
            return result;
        }

        String valueName;
        int index = 0;
        do {
            valueName = enumRegistryValue(lKey, index);
            if (valueName != null) {
                result.add(valueName);
                index++;
            }
        } while (valueName != null);

        closeRegistryKey(lKey);

        return result;
    }

    /**
     * Reads DWORD registry value.
     *
     * @param key one of HKEY predefine value
     * @param subKey registry sub key
     * @param value value to read
     * @param defaultValue default value in case if subKey or value not found
     *                     or any other errors occurred
     * @return value's data only if it was read successfully, otherwise
     *         defaultValue
     */
    private static native int readDwordValue(int key, String subKey,
            String value, int defaultValue);

    /**
     * Open registry key.
     *
     * @param key one of HKEY predefine value
     * @param subKey registry sub key
     * @return native handle to open key
     */
    private static native long openRegistryKey(int key, String subKey);

    /**
     * Enumerates the values for registry key.
     *
     * @param lKey native handle to open key returned by openRegistryKey
     * @param index index of value starting from 0. Increment until this
     *              function returns NULL which means no more values.
     * @return returns value or NULL if error or no more data
     */
    private static native String enumRegistryValue(long lKey, int index);

    /**
     * Close registry key.
     *
     * @param lKey native handle to open key returned by openRegistryKey
     */
    private static native void closeRegistryKey(long lKey);

    /**
     * Compares two Windows paths regardless case and if paths
     * are short or long.
     *
     * @param path1 path to compare
     * @param path2 path to compare
     * @return true if paths point to same location
     */
    public static native boolean comparePaths(String path1, String path2);
}
