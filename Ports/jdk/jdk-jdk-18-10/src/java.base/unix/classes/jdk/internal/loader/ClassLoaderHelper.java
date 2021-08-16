/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.loader;

import java.io.File;
import java.util.ArrayList;

class ClassLoaderHelper {

    private ClassLoaderHelper() {}

    /**
     * Returns an alternate path name for the given file
     * such that if the original pathname did not exist, then the
     * file may be located at the alternate location.
     * For most platforms, this behavior is not supported and returns null.
     */
    static File mapAlternativeName(File lib) {
        return null;
    }

    /**
     * Parse a PATH env variable.
     *
     * Empty elements will be replaced by dot.
     */
    static String[] parsePath(String ldPath) {
        char ps = File.pathSeparatorChar;
        ArrayList<String> paths = new ArrayList<>();
        int pathStart = 0;
        int pathEnd;
        while ((pathEnd = ldPath.indexOf(ps, pathStart)) >= 0) {
            paths.add((pathStart < pathEnd) ?
                    ldPath.substring(pathStart, pathEnd) : ".");
            pathStart = pathEnd + 1;
        }
        int ldLen = ldPath.length();
        paths.add((pathStart < ldLen) ?
                ldPath.substring(pathStart, ldLen) : ".");
        return paths.toArray(new String[paths.size()]);
    }
}
