/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import java.text.MessageFormat;


final class MsiVersion {
    /**
     * Parse the given string as Windows MSI Product version.
     * https://msdn.microsoft.com/en-us/library/aa370859%28v=VS.85%29.aspx The
     * format of the string is as follows: major.minor.build. The first field is
     * the major version and has a maximum value of 255. The second field is the
     * minor version and has a maximum value of 255. The third field is called
     * the build version or the update version and has a maximum value of
     * 65,535.
     * @throws IllegalArgumentException
     */
    static DottedVersion of(String value) {
        DottedVersion ver = new DottedVersion(value);

        BigInteger[] components = ver.getComponents();

        if (components.length < 2 || components.length > 4) {
            throw new IllegalArgumentException(MessageFormat.format(
                    I18N.getString("error.msi-product-version-components"),
                    value));
        }

        if (BigInteger.valueOf(255).compareTo(components[0]) < 0) {
            throw new IllegalArgumentException(I18N.getString(
                    "error.msi-product-version-major-out-of-range"));
        }

        if (components.length > 1 && BigInteger.valueOf(255).compareTo(
                components[1]) < 0) {
            throw new IllegalArgumentException(I18N.getString(
                    "error.msi-product-version-minor-out-of-range"));
        }

        if (components.length > 2 && BigInteger.valueOf(65535).compareTo(
                components[2]) < 0) {
            throw new IllegalArgumentException(I18N.getString(
                    "error.msi-product-version-build-out-of-range"));
        }

        return ver;
    }
}
