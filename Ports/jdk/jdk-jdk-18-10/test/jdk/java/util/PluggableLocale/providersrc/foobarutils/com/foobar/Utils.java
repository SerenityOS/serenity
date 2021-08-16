/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
/*
 *
 */
package com.foobar;
import java.util.*;

public class Utils {
    public static boolean supportsLocale(Locale supported, Locale requested) {
        if (supported.getLanguage() == "") {
            return true;
        } else if (supported.getLanguage() != requested.getLanguage()) {
            return false;
        }

        if (supported.getCountry() == "") {
            return true;
        } else if (supported.getCountry() != requested.getCountry()) {
            return false;
        }

        String supVar = supported.getVariant();
        String reqVar = requested.getVariant();

        if (supVar == "") {
            return true;
        } else {
            int underIndex;
            while ((underIndex = reqVar.lastIndexOf('_')) != (-1)) {
                reqVar = reqVar.substring(0, underIndex);
                if (supVar.equals(reqVar)) {
                    return true;
                }
            }
            return supVar.equals(reqVar);
        }
    }

    public static boolean supportsLocale(List<Locale> supported, Locale requested) {
        for (Locale l : supported) {
            if (supportsLocale(l, requested)) {
                return true;
            }
        }
        return false;
    }
}
