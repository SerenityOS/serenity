/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import static sun.awt.OSInfo.OSType.*;

/**
 * @author Pavel Porvatov
 */
public class OSInfo {
    public static enum OSType {
        WINDOWS,
        LINUX,
        MACOSX,
        AIX,
        UNKNOWN
    }

    /*
       The map windowsVersionMap must contain all windows version constants except WINDOWS_UNKNOWN,
       and so the method getWindowsVersion() will return the constant for known OS.
       It allows compare objects by "==" instead of "equals".
     */
    public static final WindowsVersion WINDOWS_UNKNOWN = new WindowsVersion(-1, -1);
    public static final WindowsVersion WINDOWS_95 = new WindowsVersion(4, 0);
    public static final WindowsVersion WINDOWS_98 = new WindowsVersion(4, 10);
    public static final WindowsVersion WINDOWS_ME = new WindowsVersion(4, 90);
    public static final WindowsVersion WINDOWS_2000 = new WindowsVersion(5, 0);
    public static final WindowsVersion WINDOWS_XP = new WindowsVersion(5, 1);
    public static final WindowsVersion WINDOWS_2003 = new WindowsVersion(5, 2);
    public static final WindowsVersion WINDOWS_VISTA = new WindowsVersion(6, 0);
    public static final WindowsVersion WINDOWS_7 = new WindowsVersion(6, 1);

    private static final String OS_NAME = "os.name";
    private static final String OS_VERSION = "os.version";

    private static final Map<String, WindowsVersion> windowsVersionMap = new HashMap<String, OSInfo.WindowsVersion>();

    static {
        windowsVersionMap.put(WINDOWS_95.toString(), WINDOWS_95);
        windowsVersionMap.put(WINDOWS_98.toString(), WINDOWS_98);
        windowsVersionMap.put(WINDOWS_ME.toString(), WINDOWS_ME);
        windowsVersionMap.put(WINDOWS_2000.toString(), WINDOWS_2000);
        windowsVersionMap.put(WINDOWS_XP.toString(), WINDOWS_XP);
        windowsVersionMap.put(WINDOWS_2003.toString(), WINDOWS_2003);
        windowsVersionMap.put(WINDOWS_VISTA.toString(), WINDOWS_VISTA);
        windowsVersionMap.put(WINDOWS_7.toString(), WINDOWS_7);
    }

    private static final PrivilegedAction<OSType> osTypeAction = new PrivilegedAction<OSType>() {
        public OSType run() {
            return getOSType();
        }
    };

    private OSInfo() {
        // Don't allow to create instances
    }

    /**
     * Returns type of operating system.
     */
    public static OSType getOSType() throws SecurityException {
        String osName = System.getProperty(OS_NAME);

        if (osName != null) {
            if (osName.contains("Windows")) {
                return WINDOWS;
            }

            if (osName.contains("Linux")) {
                return LINUX;
            }

            if (osName.contains("OS X")) {
                return MACOSX;
            }

            if (osName.contains("AIX")) {
                return AIX;
            }

            // determine another OS here
        }

        return UNKNOWN;
    }

    public static PrivilegedAction<OSType> getOSTypeAction() {
        return osTypeAction;
    }

    public static WindowsVersion getWindowsVersion() throws SecurityException {
        String osVersion = System.getProperty(OS_VERSION);

        if (osVersion == null) {
            return WINDOWS_UNKNOWN;
        }

        synchronized (windowsVersionMap) {
            WindowsVersion result = windowsVersionMap.get(osVersion);

            if (result == null) {
                // Try parse version and put object into windowsVersionMap
                String[] arr = osVersion.split("\\.");

                if (arr.length == 2) {
                    try {
                        result = new WindowsVersion(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]));
                    } catch (NumberFormatException e) {
                        return WINDOWS_UNKNOWN;
                    }
                } else {
                    return WINDOWS_UNKNOWN;
                }

                windowsVersionMap.put(osVersion, result);
            }

            return result;
        }
    }

    public static class WindowsVersion implements Comparable<WindowsVersion> {
        private final int major;

        private final int minor;

        private WindowsVersion(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        public int compareTo(WindowsVersion o) {
            int result = major - o.getMajor();

            if (result == 0) {
                result = minor - o.getMinor();
            }

            return result;
        }

        public boolean equals(Object obj) {
            return obj instanceof WindowsVersion && compareTo((WindowsVersion) obj) == 0;
        }

        public int hashCode() {
            return 31 * major + minor;
        }

        public String toString() {
            return major + "." + minor;
        }
    }
}
