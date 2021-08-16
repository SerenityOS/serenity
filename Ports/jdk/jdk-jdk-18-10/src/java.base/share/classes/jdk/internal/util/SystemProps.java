/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.util;


import java.lang.annotation.Native;
import java.util.HashMap;
import java.util.Map;

/**
 * System Property initialization for internal use only
 * Retrieves the platform, JVM, and command line properties,
 * applies initial defaults and returns the Properties instance
 * that becomes the System.getProperties instance.
 */
public final class SystemProps {

    // no instances
    private SystemProps() {}

    /**
     * Create and initialize the system properties from the native properties
     * and command line properties.
     * Note:  Build-defined properties such as versions and vendor information
     * are initialized by VersionProps.java-template.
     *
     * @return a Properties instance initialized with all of the properties
     */
    public static Map<String, String> initProperties() {

        // Initially, cmdProperties only includes -D and props from the VM
        Raw raw = new Raw();
        HashMap<String, String> props = raw.cmdProperties();

        String javaHome = props.get("java.home");
        assert javaHome != null : "java.home not set";

        putIfAbsent(props, "user.home", raw.propDefault(Raw._user_home_NDX));
        putIfAbsent(props, "user.dir", raw.propDefault(Raw._user_dir_NDX));
        putIfAbsent(props, "user.name", raw.propDefault(Raw._user_name_NDX));

        // Platform defined encoding cannot be overridden on the command line
        put(props, "sun.jnu.encoding", raw.propDefault(Raw._sun_jnu_encoding_NDX));
        var nativeEncoding = ((raw.propDefault(Raw._file_encoding_NDX) == null)
                ? raw.propDefault(Raw._sun_jnu_encoding_NDX)
                : raw.propDefault(Raw._file_encoding_NDX));
        put(props, "native.encoding", nativeEncoding);

        // Add properties that have not been overridden on the cmdline
        putIfAbsent(props, "file.encoding", nativeEncoding);

        // Use platform values if not overridden by a commandline -Dkey=value
        // In no particular order
        putIfAbsent(props, "os.name", raw.propDefault(Raw._os_name_NDX));
        putIfAbsent(props, "os.arch", raw.propDefault(Raw._os_arch_NDX));
        putIfAbsent(props, "os.version", raw.propDefault(Raw._os_version_NDX));
        putIfAbsent(props, "line.separator", raw.propDefault(Raw._line_separator_NDX));
        putIfAbsent(props, "file.separator", raw.propDefault(Raw._file_separator_NDX));
        putIfAbsent(props, "path.separator", raw.propDefault(Raw._path_separator_NDX));
        putIfAbsent(props, "java.io.tmpdir", raw.propDefault(Raw._java_io_tmpdir_NDX));
        putIfAbsent(props, "http.proxyHost", raw.propDefault(Raw._http_proxyHost_NDX));
        putIfAbsent(props, "http.proxyPort", raw.propDefault(Raw._http_proxyPort_NDX));
        putIfAbsent(props, "https.proxyHost", raw.propDefault(Raw._https_proxyHost_NDX));
        putIfAbsent(props, "https.proxyPort", raw.propDefault(Raw._https_proxyPort_NDX));
        putIfAbsent(props, "ftp.proxyHost", raw.propDefault(Raw._ftp_proxyHost_NDX));
        putIfAbsent(props, "ftp.proxyPort", raw.propDefault(Raw._ftp_proxyPort_NDX));
        putIfAbsent(props, "socksProxyHost", raw.propDefault(Raw._socksProxyHost_NDX));
        putIfAbsent(props, "socksProxyPort", raw.propDefault(Raw._socksProxyPort_NDX));
        putIfAbsent(props, "http.nonProxyHosts", raw.propDefault(Raw._http_nonProxyHosts_NDX));
        putIfAbsent(props, "ftp.nonProxyHosts", raw.propDefault(Raw._ftp_nonProxyHosts_NDX));
        putIfAbsent(props, "socksNonProxyHosts", raw.propDefault(Raw._socksNonProxyHosts_NDX));
        putIfAbsent(props, "sun.arch.abi", raw.propDefault(Raw._sun_arch_abi_NDX));
        putIfAbsent(props, "sun.arch.data.model", raw.propDefault(Raw._sun_arch_data_model_NDX));
        putIfAbsent(props, "sun.os.patch.level", raw.propDefault(Raw._sun_os_patch_level_NDX));
        putIfAbsent(props, "sun.stdout.encoding", raw.propDefault(Raw._sun_stdout_encoding_NDX));
        putIfAbsent(props, "sun.stderr.encoding", raw.propDefault(Raw._sun_stderr_encoding_NDX));
        putIfAbsent(props, "sun.io.unicode.encoding", raw.propDefault(Raw._sun_io_unicode_encoding_NDX));
        putIfAbsent(props, "sun.cpu.isalist", raw.propDefault(Raw._sun_cpu_isalist_NDX));
        putIfAbsent(props, "sun.cpu.endian", raw.propDefault(Raw._sun_cpu_endian_NDX));

        /* Construct i18n related options */
        fillI18nProps(props,"user.language", raw.propDefault(Raw._display_language_NDX),
                raw.propDefault(Raw._format_language_NDX));
        fillI18nProps(props,"user.script",   raw.propDefault(Raw._display_script_NDX),
                raw.propDefault(Raw._format_script_NDX));
        fillI18nProps(props,"user.country",  raw.propDefault(Raw._display_country_NDX),
                raw.propDefault(Raw._format_country_NDX));
        fillI18nProps(props,"user.variant",  raw.propDefault(Raw._display_variant_NDX),
                raw.propDefault(Raw._format_variant_NDX));

        return props;
    }

    /**
     * Puts the property if it is non-null
     * @param props the Properties
     * @param key the key
     * @param value the value
     */
    private static void put(HashMap<String, String> props, String key, String value) {
        if (value != null) {
            props.put(key, value);
        }
    }

    /**
     * Puts the property if it is non-null and is not already in the Properties.
     * @param props the Properties
     * @param key the key
     * @param value the value
     */
    private static void putIfAbsent(HashMap<String, String> props, String key, String value) {
        if (value != null) {
            props.putIfAbsent(key, value);
        }
    }

    /**
     * For internationalization options, compute the values for
     * display and format properties
     * MUST NOT override command line defined values.
     *
     * @param base the base property name
     * @param display the display value for the base
     * @param format the format value for the base
     */
    private static void fillI18nProps(HashMap<String, String> cmdProps,
                                      String base,
                                      String display,
                                      String format) {
        // Do not override command line setting
        String baseValue = cmdProps.get(base);
        if (baseValue != null) {
            return;     // Do not override value from the command line
        }

        // Not overridden on the command line; define the properties if there are platform defined values
        if (display != null) {
            cmdProps.put(base, display);
            baseValue = display;
        }

        /* user.xxx.display property */
        String disp = base.concat(".display");
        String dispValue = cmdProps.get(disp);
        if (dispValue == null && display != null && !display.equals(baseValue)) {
            // Create the property only if different from the base property
            cmdProps.put(disp, display);
        }

        /* user.xxx.format property */
        String fmt = base.concat(".format");
        String fmtValue = cmdProps.get(fmt);
        if (fmtValue == null && format != null && !format.equals(baseValue)) {
            // Create the property only if different than the base property
            cmdProps.put(fmt, format);
        }
    }

    /**
     * Read the raw properties from native System.c.
     */
    public static class Raw {
        // Array indices written by native vmProperties()
        // The order is arbitrary (but alphabetic for convenience)
        @Native private static final int _display_country_NDX = 0;
        @Native private static final int _display_language_NDX = 1 + _display_country_NDX;
        @Native private static final int _display_script_NDX = 1 + _display_language_NDX;
        @Native private static final int _display_variant_NDX = 1 + _display_script_NDX;
        @Native private static final int _file_encoding_NDX = 1 + _display_variant_NDX;
        @Native private static final int _file_separator_NDX = 1 + _file_encoding_NDX;
        @Native private static final int _format_country_NDX = 1 + _file_separator_NDX;
        @Native private static final int _format_language_NDX = 1 + _format_country_NDX;
        @Native private static final int _format_script_NDX = 1 + _format_language_NDX;
        @Native private static final int _format_variant_NDX = 1 + _format_script_NDX;
        @Native private static final int _ftp_nonProxyHosts_NDX = 1 + _format_variant_NDX;
        @Native private static final int _ftp_proxyHost_NDX = 1 + _ftp_nonProxyHosts_NDX;
        @Native private static final int _ftp_proxyPort_NDX = 1 + _ftp_proxyHost_NDX;
        @Native private static final int _http_nonProxyHosts_NDX = 1 + _ftp_proxyPort_NDX;
        @Native private static final int _http_proxyHost_NDX = 1 + _http_nonProxyHosts_NDX;
        @Native private static final int _http_proxyPort_NDX = 1 + _http_proxyHost_NDX;
        @Native private static final int _https_proxyHost_NDX = 1 + _http_proxyPort_NDX;
        @Native private static final int _https_proxyPort_NDX = 1 + _https_proxyHost_NDX;
        @Native private static final int _java_io_tmpdir_NDX = 1 + _https_proxyPort_NDX;
        @Native private static final int _line_separator_NDX = 1 + _java_io_tmpdir_NDX;
        @Native private static final int _os_arch_NDX = 1 + _line_separator_NDX;
        @Native private static final int _os_name_NDX = 1 + _os_arch_NDX;
        @Native private static final int _os_version_NDX = 1 + _os_name_NDX;
        @Native private static final int _path_separator_NDX = 1 + _os_version_NDX;
        @Native private static final int _socksNonProxyHosts_NDX = 1 + _path_separator_NDX;
        @Native private static final int _socksProxyHost_NDX = 1 + _socksNonProxyHosts_NDX;
        @Native private static final int _socksProxyPort_NDX = 1 + _socksProxyHost_NDX;
        @Native private static final int _sun_arch_abi_NDX = 1 + _socksProxyPort_NDX;
        @Native private static final int _sun_arch_data_model_NDX = 1 + _sun_arch_abi_NDX;
        @Native private static final int _sun_cpu_endian_NDX = 1 + _sun_arch_data_model_NDX;
        @Native private static final int _sun_cpu_isalist_NDX = 1 + _sun_cpu_endian_NDX;
        @Native private static final int _sun_io_unicode_encoding_NDX = 1 + _sun_cpu_isalist_NDX;
        @Native private static final int _sun_jnu_encoding_NDX = 1 + _sun_io_unicode_encoding_NDX;
        @Native private static final int _sun_os_patch_level_NDX = 1 + _sun_jnu_encoding_NDX;
        @Native private static final int _sun_stderr_encoding_NDX = 1 + _sun_os_patch_level_NDX;
        @Native private static final int _sun_stdout_encoding_NDX = 1 + _sun_stderr_encoding_NDX;
        @Native private static final int _user_dir_NDX = 1 + _sun_stdout_encoding_NDX;
        @Native private static final int _user_home_NDX = 1 + _user_dir_NDX;
        @Native private static final int _user_name_NDX = 1 + _user_home_NDX;
        @Native private static final int FIXED_LENGTH = 1 + _user_name_NDX;

        // Array of Strings returned from the VM and Command line properties
        // The array is not used after initialization is complete.
        private final String[] platformProps;

        private Raw() {
            platformProps = platformProperties();
        }

        /**
         * Return the value for a well known default from native.
         * @param index the index of the known property
         * @return the value
         */
        String propDefault(int index) {
            return platformProps[index];
        }

        /**
         * Return a Properties instance of the command line and VM options
         * defined by name and value.
         * The Properties instance is sized to include the fixed properties.
         *
         * @return return a Properties instance of the command line and VM options
         */
        private HashMap<String, String> cmdProperties() {
            String[] vmProps = vmProperties();
            // While optimal initialCapacity here would be the exact number of properties
            // divided by LOAD_FACTOR, a large portion of the properties in Raw are
            // usually not set, so for typical cases the chosen capacity avoids resizing
            var cmdProps = new HashMap<String, String>((vmProps.length / 2) + Raw.FIXED_LENGTH);
            for (int i = 0; i < vmProps.length;) {
                String k = vmProps[i++];
                if (k != null) {
                    String v = vmProps[i++];
                    cmdProps.put(k, v != null ? v : "");
                } else {
                    // no more key/value pairs
                    break;
                }
            }
            return cmdProps;
        }

        /**
         * Returns the available VM and Command Line Properties.
         * The VM supplies some key/value pairs and processes the command line
         * to extract key/value pairs from the {@code "-Dkey=value"} arguments.
         *
         * @return an array of strings, with alternating key and value strings.
         *      Either keys or values may be null, the array may not be full.
         *      The first null key indicates there are no more key, value pairs.
         */
        private static native String[] vmProperties();

        /**
         * Returns the platform specific property values identified
         * by {@code "_xxx_NDX"} indexes.
         * The indexes are strictly private, to be shared only with the native code.
         *
         * @return a array of strings, the properties are indexed by the {@code _xxx_NDX}
         * indexes.  The values are Strings and may be null.
         */
        private static native String[] platformProperties();
    }
}
