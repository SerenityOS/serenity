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

package jdk.test.lib;

import java.util.Arrays;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

public final class OSVersion implements Comparable<OSVersion> {
    public static final OSVersion WINDOWS_95 = new OSVersion(4, 0);
    public static final OSVersion WINDOWS_98 = new OSVersion(4, 10);
    public static final OSVersion WINDOWS_ME = new OSVersion(4, 90);
    public static final OSVersion WINDOWS_2000 = new OSVersion(5, 0);
    public static final OSVersion WINDOWS_XP = new OSVersion(5, 1);
    public static final OSVersion WINDOWS_2003 = new OSVersion(5, 2);
    public static final OSVersion WINDOWS_VISTA = new OSVersion(6, 0);

    private final int[] versionTokens;

    public static OSVersion current() {
        return new OSVersion(Platform.getOsVersion());
    }

    public OSVersion(int major, int minor) {
        versionTokens = new int[] {major, minor};
    }

    public OSVersion(String version) {
        Pattern onlyDigits = Pattern.compile("^\\d+$");
        this.versionTokens = Arrays.stream(version.split("-")[0].split("\\."))
                                   .filter(onlyDigits.asPredicate())
                                   .mapToInt(Integer::parseInt)
                                   .toArray();
    }

    @Override
    public int compareTo(OSVersion o) {
        return Arrays.compare(this.versionTokens, o.versionTokens);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(versionTokens);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OSVersion osVersion = (OSVersion) o;
        return Arrays.equals(versionTokens, osVersion.versionTokens);
    }

    @Override
    public String toString() {
        return Arrays.stream(versionTokens)
                     .mapToObj(String::valueOf)
                     .collect(Collectors.joining("."));
    }
}

