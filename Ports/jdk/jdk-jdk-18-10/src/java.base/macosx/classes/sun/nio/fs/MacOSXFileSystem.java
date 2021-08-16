/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.util.regex.Pattern;

import static sun.nio.fs.MacOSXNativeDispatcher.*;

/**
 * MacOS implementation of FileSystem
 */

class MacOSXFileSystem extends BsdFileSystem {

    MacOSXFileSystem(UnixFileSystemProvider provider, String dir) {
        super(provider, dir);
    }

    // match in unicode canon_eq
    Pattern compilePathMatchPattern(String expr) {
        return Pattern.compile(expr, Pattern.CANON_EQ) ;
    }

    @Override
    String normalizeNativePath(String path) {
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c > 0x80)
                return new String(normalizepath(path.toCharArray(),
                                  kCFStringNormalizationFormD));
        }
        return path;
    }

    @Override
    String normalizeJavaPath(String path) {
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) > 0x80)
                return new String(normalizepath(path.toCharArray(),
                                  kCFStringNormalizationFormC));
        }
        return path;
    }

}
