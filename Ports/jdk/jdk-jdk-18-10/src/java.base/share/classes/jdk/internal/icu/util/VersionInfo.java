/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
/*
 *******************************************************************************
 * (C) Copyright IBM Corp. and others, 1996-2009 - All Rights Reserved         *
 *                                                                             *
 * The original version of this source code and documentation is copyrighted   *
 * and owned by IBM, These materials are provided under terms of a License     *
 * Agreement between IBM and Sun. This technology is protected by multiple     *
 * US and International patents. This notice and attribution to IBM may not    *
 * to removed.                                                                 *
 *******************************************************************************
 */

package jdk.internal.icu.util;

import java.util.HashMap;

/**
 * Class to store version numbers of the form major.minor.milli.micro.
 * @author synwee
 * @stable ICU 2.6
 */
public final class VersionInfo
{
    // public data members -------------------------------------------------

    /**
     * Data version string for ICU's internal data.
     * Used for appending to data path (e.g. icudt43b)
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static final String ICU_DATA_VERSION_PATH = "67b";

    // public methods ------------------------------------------------------

    /**
     * Returns an instance of VersionInfo with the argument version.
     * @param version version String in the format of "major.minor.milli.micro"
     *                or "major.minor.milli" or "major.minor" or "major",
     *                where major, minor, milli, micro are non-negative numbers
     *                {@literal <=} 255. If the trailing version numbers are
     *                not specified they are taken as 0s. E.g. Version "3.1" is
     *                equivalent to "3.1.0.0".
     * @return an instance of VersionInfo with the argument version.
     * @exception throws an IllegalArgumentException when the argument version
     *                is not in the right format
     * @stable ICU 2.6
     */
    public static VersionInfo getInstance(String version)
    {
        int length  = version.length();
        int array[] = {0, 0, 0, 0};
        int count   = 0;
        int index   = 0;

        while (count < 4 && index < length) {
            char c = version.charAt(index);
            if (c == '.') {
                count ++;
            }
            else {
                c -= '0';
                if (c < 0 || c > 9) {
                    throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
                }
                array[count] *= 10;
                array[count] += c;
            }
            index ++;
        }
        if (index != length) {
            throw new IllegalArgumentException(
                                               "Invalid version number: String '" + version + "' exceeds version format");
        }
        for (int i = 0; i < 4; i ++) {
            if (array[i] < 0 || array[i] > 255) {
                throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
            }
        }

        return getInstance(array[0], array[1], array[2], array[3]);
    }

    /**
     * Returns an instance of VersionInfo with the argument version.
     * @param major major version, non-negative number {@literal <=} 255.
     * @param minor minor version, non-negative number {@literal <=} 255.
     * @param milli milli version, non-negative number {@literal <=} 255.
     * @param micro micro version, non-negative number {@literal <=} 255.
     * @exception throws an IllegalArgumentException when either arguments are
     *                                     negative or {@literal >} 255
     * @stable ICU 2.6
     */
    public static VersionInfo getInstance(int major, int minor, int milli,
                                          int micro)
    {
        // checks if it is in the hashmap
        // else
        if (major < 0 || major > 255 || minor < 0 || minor > 255 ||
            milli < 0 || milli > 255 || micro < 0 || micro > 255) {
            throw new IllegalArgumentException(INVALID_VERSION_NUMBER_);
        }
        int     version = getInt(major, minor, milli, micro);
        Integer key     = Integer.valueOf(version);
        Object  result  = MAP_.get(key);
        if (result == null) {
            result = new VersionInfo(version);
            MAP_.put(key, result);
        }
        return (VersionInfo)result;
    }

    /**
     * Compares other with this VersionInfo.
     * @param other VersionInfo to be compared
     * @return 0 if the argument is a VersionInfo object that has version
     *           information equal to this object.
     *           Less than 0 if the argument is a VersionInfo object that has
     *           version information greater than this object.
     *           Greater than 0 if the argument is a VersionInfo object that
     *           has version information less than this object.
     * @stable ICU 2.6
     */
    public int compareTo(VersionInfo other)
    {
        return m_version_ - other.m_version_;
    }

    // private data members ----------------------------------------------

    /**
     * Version number stored as a byte for each of the major, minor, milli and
     * micro numbers in the 32 bit int.
     * Most significant for the major and the least significant contains the
     * micro numbers.
     */
    private int m_version_;
    /**
     * Map of singletons
     */
    private static final HashMap<Integer, Object> MAP_ = new HashMap<>();
    /**
     * Error statement string
     */
    private static final String INVALID_VERSION_NUMBER_ =
        "Invalid version number: Version number may be negative or greater than 255";

    // private constructor -----------------------------------------------

    /**
     * Constructor with int
     * @param compactversion a 32 bit int with each byte representing a number
     */
    private VersionInfo(int compactversion)
    {
        m_version_ = compactversion;
    }

    /**
     * Gets the int from the version numbers
     * @param major non-negative version number
     * @param minor non-negativeversion number
     * @param milli non-negativeversion number
     * @param micro non-negativeversion number
     */
    private static int getInt(int major, int minor, int milli, int micro)
    {
        return (major << 24) | (minor << 16) | (milli << 8) | micro;
    }
}
