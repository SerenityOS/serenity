/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.whitebox.cpuinfo;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jdk.test.whitebox.WhiteBox;

/**
 * Information about CPU on test box.
 *
 * CPUInfo uses WhiteBox to gather information,
 * so WhiteBox class should be added to bootclasspath
 * and option -XX:+WhiteBoxAPI should be explicitly
 * specified on command line.
 */
public class CPUInfo {

    private static final List<String> features;
    private static final String additionalCPUInfo;

    static {
        WhiteBox wb = WhiteBox.getWhiteBox();

        Pattern additionalCPUInfoRE =
            Pattern.compile("([^(]*\\([^)]*\\)[^,]*),\\s*");

        String cpuFeaturesString = wb.getCPUFeatures();
        Matcher matcher = additionalCPUInfoRE.matcher(cpuFeaturesString);
        if (matcher.find()) {
            additionalCPUInfo = matcher.group(1);
        } else {
            additionalCPUInfo = "";
        }
        String splittedFeatures[] = matcher.replaceAll("").split("(, )| ");

        features = Collections.unmodifiableList(Arrays.
                                                asList(splittedFeatures));
    }

    /**
     * Get additional information about CPU.
     * For example, on X86 in will be family/model/stepping
     * and number of cores.
     *
     * @return additional CPU info
     */
    public static String getAdditionalCPUInfo() {
        return additionalCPUInfo;
    }

    /**
     * Get all known features supported by CPU.
     *
     * @return unmodifiable list with names of all known features
     *         supported by CPU.
     */
    public static List<String> getFeatures() {
        return features;
    }

    /**
     * Check if some feature is supported by CPU.
     *
     * @param feature Name of feature to be tested.
     * @return <b>true</b> if tested feature is supported by CPU.
     */
    public static boolean hasFeature(String feature) {
        return features.contains(feature.toLowerCase());
    }
}
