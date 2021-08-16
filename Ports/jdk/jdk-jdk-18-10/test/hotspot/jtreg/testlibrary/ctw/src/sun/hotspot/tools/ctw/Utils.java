/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.hotspot.tools.ctw;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;

/**
 * Auxiliary methods.
 */
public class Utils {
    /**
     * Value of {@code -XX:CompileThreshold}
     */
    public static final boolean TIERED_COMPILATION
            = Boolean.parseBoolean(getVMOption("TieredCompilation", "false"));
    /**
     * Value of {@code -XX:BackgroundCompilation}
     */
    public static final boolean BACKGROUND_COMPILATION
            = Boolean.parseBoolean(getVMOption("BackgroundCompilation",
            "false"));
    /**
     * Value of {@code -XX:TieredStopAtLevel}
     */
    public static final int TIERED_STOP_AT_LEVEL;
    /**
     * Value of {@code -XX:CICompilerCount}
     */
    public static final Integer CI_COMPILER_COUNT
            = Integer.valueOf(getVMOption("CICompilerCount", "1"));
    /**
     * Initial compilation level.
     */
    public static final int INITIAL_COMP_LEVEL;
    /**
     * Value of {@code -DCompileTheWorldStopAt}. Last class to consider.
     */
    public static final long COMPILE_THE_WORLD_STOP_AT
            = Long.getLong("CompileTheWorldStopAt", Long.MAX_VALUE);
    /**
     * Value of {@code -DCompileTheWorldStartAt}. First class to consider.
     */
    public static final long COMPILE_THE_WORLD_START_AT
            = Long.getLong("CompileTheWorldStartAt", 1);
    /**
     * Value of {@code -DCompileTheWorldPreloadClasses}. Preload all classes
     * used by a class before start loading.
     */
    public static final boolean COMPILE_THE_WORLD_PRELOAD_CLASSES;
    /**
     * Value of {@code -Dsun.hotspot.tools.ctw.verbose}. Verbose output,
     * adds additional information about compilation.
     */
    public static final boolean IS_VERBOSE
            = Boolean.getBoolean("sun.hotspot.tools.ctw.verbose");
    /**
     * Value of {@code -Dsun.hotspot.tools.ctw.logfile}.Path to logfile, if
     * it's null, cout will be used.
     */
    public static final String LOG_FILE
            = System.getProperty("sun.hotspot.tools.ctw.logfile");
    static {
        if (Utils.TIERED_COMPILATION) {
            INITIAL_COMP_LEVEL = 1;
        } else {
            String vmName = System.getProperty("java.vm.name");
            String vmInfo = System.getProperty("java.vm.info");
            boolean isEmulatedClient = (vmInfo != null) && vmInfo.contains("emulated-client");
            if (Utils.endsWithIgnoreCase(vmName, " Server VM") && !isEmulatedClient) {
                INITIAL_COMP_LEVEL = 4;
            } else if (Utils.endsWithIgnoreCase(vmName, " Client VM")
                    || Utils.endsWithIgnoreCase(vmName, " Minimal VM") || isEmulatedClient) {
                INITIAL_COMP_LEVEL = 1;
            } else {
                throw new RuntimeException("Unknown VM: " + vmName);
            }
        }

        TIERED_STOP_AT_LEVEL = Integer.parseInt(getVMOption("TieredStopAtLevel",
                String.valueOf(INITIAL_COMP_LEVEL)));
    }

    static {
        String tmp = System.getProperty("CompileTheWorldPreloadClasses");
        if (tmp == null) {
            COMPILE_THE_WORLD_PRELOAD_CLASSES = true;
        } else {
            COMPILE_THE_WORLD_PRELOAD_CLASSES = Boolean.parseBoolean(tmp);
        }
    }

    public static final String CLASSFILE_EXT = ".class";

    private Utils() {
    }

    /**
     * Tests if the string ends with the suffix, ignoring case
     * considerations
     *
     * @param string the tested string
     * @param suffix the suffix
     * @return {@code true} if {@code string} ends with the {@code suffix}
     * @see String#endsWith(String)
     */
    public static boolean endsWithIgnoreCase(String string, String suffix) {
        if (string == null || suffix == null) {
            return false;
        }
        int length = suffix.length();
        int toffset = string.length() - length;
        if (toffset < 0) {
            return false;
        }
        return string.regionMatches(true, toffset, suffix, 0, length);
    }

    /**
     * Returns value of VM option.
     *
     * @param name option's name
     * @return value of option or {@code null}, if option doesn't exist
     * @throws NullPointerException if name is null
     */
    public static String getVMOption(String name) {
        String result;
        HotSpotDiagnosticMXBean diagnostic
                = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        result = diagnostic.getVMOption(name).getValue();
        return result;
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
        String result;
        try {
            result = getVMOption(name);
        } catch (NoClassDefFoundError e) {
            // compact1, compact2 support
            result = defaultValue;
        }
        return result == null ? defaultValue : result;
    }

    /**
     * Tests if the filename is valid filename for class file.
     *
     * @param filename tested filename
     */
    public static boolean isClassFile(String filename) {
        return endsWithIgnoreCase(filename, CLASSFILE_EXT)
                // skip all module-info.class files
                && !(filename.substring(filename.lastIndexOf('/') + 1,
                                        filename.lastIndexOf('.'))
                             .equals("module-info"));
    }

    /**
     * Converts the filename to classname.
     *
     * @param filename filename to convert
     * @return corresponding classname
     * @throws AssertionError if filename isn't valid filename for class file -
     *                        {@link #isClassFile(String)}
     */
    public static String fileNameToClassName(String filename) {
        assert isClassFile(filename);
        final char nameSeparator = '/';
        return filename.substring(0, filename.length() - CLASSFILE_EXT.length())
                       .replace(nameSeparator, '.');
    }

    public static String classNameToFileName(String classname) {
        return classname.replace('.', '/')
                        .concat(CLASSFILE_EXT);
    }
}
