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


#include <limits.h>
#include <sys/stat.h>
#include <unistd.h>
#include "FileUtils.h"
#include "ErrorHandling.h"


namespace FileUtils {

bool isFileExists(const tstring &filePath) {
    struct stat statBuffer;
    return (stat(filePath.c_str(), &statBuffer) != -1);
}


tstring toAbsolutePath(const tstring& path) {
    if (path.empty()) {
        char buffer[PATH_MAX] = { 0 };
        char* buf = getcwd(buffer, sizeof(buffer));
        if (buf) {
            tstring result(buf);
            if (result.empty()) {
                JP_THROW(tstrings::any() << "getcwd() returned empty string");
            }
            return result;
        }

        JP_THROW(tstrings::any() << "getcwd() failed. Error: "
                << lastCRTError());
    }

    if (isDirSeparator(path[0])) {
        return path;
    }

    return mkpath() << toAbsolutePath("") << path;
}

tstring stripExeSuffix(const tstring& path) {
    // for unix - there is no suffix to remove
    return path;
}

} //  namespace FileUtils
