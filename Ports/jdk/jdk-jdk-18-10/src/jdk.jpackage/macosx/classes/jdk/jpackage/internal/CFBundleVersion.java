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


final class CFBundleVersion {
    /**
     * Parse the given string as OSX CFBundleVersion.
     * CFBundleVersion (String - iOS, OS X) specifies the build version number of
     * the bundle, which identifies an iteration (released or unreleased) of the
     * bundle. The build version number should be a string comprised of at most three
     * non-negative, period-separated integers with the first integer being greater
     * than zero.
     * @throws IllegalArgumentException
     */
    static DottedVersion of(String value) {
        DottedVersion ver = new DottedVersion(value);

        BigInteger[] components = ver.getComponents();
        if (components.length > 3) {
            throw new IllegalArgumentException(I18N.getString(
                    "message.version-string-too-many-components"));
        }

        if (BigInteger.ZERO.equals(components[0])) {
            throw new IllegalArgumentException(I18N.getString(
                    "message.version-string-first-number-not-zero"));
        }

        return ver;
    }
}
