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

package jdk.test.lib;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class BuildHelper {

    /**
     * Commercial builds should have the BUILD_TYPE set to commercial
     * within the release file, found at the root of the JDK.
     */
    public static boolean isCommercialBuild() throws Exception {
        String buildType = getReleaseProperty("BUILD_TYPE","notFound");
        return buildType.equals("commercial");
    }


    /**
     * Return the value for property key, or defaultValue if no property not found.
     * If present, double quotes are trimmed.
     */
    public static String getReleaseProperty(String key, String defaultValue) throws Exception {
        Properties properties = getReleaseProperties();
        String value = properties.getProperty(key, defaultValue);
        return trimDoubleQuotes(value);
    }

    /**
     * Return the value for property key, or null if no property not found.
     * If present, double quotes are trimmed.
     */
    public static String getReleaseProperty(String key) throws Exception {
        return getReleaseProperty(key, null);
    }

    /**
     * Get properties from the release file
     */
    public static Properties getReleaseProperties() throws Exception {
        Properties properties = new Properties();
        properties.load(new FileReader(getReleaseFile()));
        return properties;
    }

    /**
     * Every JDK has a release file in its root.
     * @return A handler to the release file.
     */
    public static File getReleaseFile() throws Exception {
        String jdkPath = getJDKRoot();
        File releaseFile = new File(jdkPath,"release");
        if ( ! releaseFile.canRead() ) {
            throw new Exception("Release file is not readable, or it is absent: " +
                    releaseFile.getCanonicalPath());
        }
        return releaseFile;
    }

    /**
     * Returns path to the JDK under test.
     * This path is obtained through the test.jdk property, usually set by JTREG.
     */
    public static String getJDKRoot() {
        String jdkPath = System.getProperty("test.jdk");
        if (jdkPath == null) {
            throw new RuntimeException("System property 'test.jdk' not set. This property is normally set by jtreg. "
                    + "When running test separately, set this property using '-Dtest.jdk=/path/to/jdk'.");
        }
        return jdkPath;
    }

    /**
     * Trim double quotes from the beginning and the end of the given string.
     * @param original string to trim.
     * @return a new trimmed string.
     */
    public static String trimDoubleQuotes(String original) {
        if (original == null) { return null; }
        String trimmed = original.replaceAll("^\"+|\"+$", "");
        return trimmed;
    }
}
