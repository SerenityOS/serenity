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

#ifndef FILEUTILS_H
#define FILEUTILS_H


#include <fstream>
#include "SysInfo.h"


namespace FileUtils {
    extern const tstring::value_type pathSeparator;

    // Returns 'true' if the given character is a path separator.
    bool isDirSeparator(const tstring::value_type c);

    // returns directory part of the path.
    // returns empty string if the path contains only filename.
    // if the path ends with slash/backslash,
    // returns removeTrailingSlashes(path).
    tstring dirname(const tstring &path);

    // returns basename part of the path
    // if the path ends with slash/backslash, returns empty string.
    tstring basename(const tstring &path);

    /**
     * Translates forward slashes to back slashes and returns lower case version
     * of the given string.
     */
    tstring normalizePath(tstring v);

    // Returns suffix of the path. If the given path has a suffix the first
    // character of the return value is '.'.
    // Otherwise return value if empty string.
    tstring suffix(const tstring &path);

    // combines two strings into a path
    tstring combinePath(const tstring& parent, const tstring& child);

    // removes trailing slashes and backslashes in the path if any
    tstring removeTrailingSlash(const tstring& path);

    /**
     * Replace file suffix, example replaceSuffix("file/path.txt", ".csv")
     * @param path file path to replace suffix
     * @param suffix new suffix for path
     * @return return file path with new suffix
     */
    tstring replaceSuffix(const tstring& path, const tstring& suffix=tstring());

    // remove the executable suffix if there is one
    tstring stripExeSuffix(const tstring& path);

    /**
     * Returns absolute path of the given path.
     * If the given string is empty, returns absolute path to the current
     * directory.
     */
    tstring toAbsolutePath(const tstring& path);

    // Helper to construct path from multiple components.
    //
    // Sample usage:
    //  Construct "c:\Program Files\Java" string from three components
    //
    //  tstring path = FileUtils::mkpath()  << _T("c:")
    //                                      << _T("Program Files")
    //                                      << _T("Java");
    //
    class mkpath {
    public:
        operator const tstring& () const {
            return path;
        }

        mkpath& operator << (const tstring& p) {
            path = combinePath(path, p);
            return *this;
        }

        // mimic std::string
        const tstring::value_type* c_str() const {
            return path.c_str();
        }
    private:
        tstring path;
    };

    // checks if the file or directory exists
    bool isFileExists(const tstring &filePath);

    // checks is the specified file is a directory
    // returns false if the path does not exist
    bool isDirectory(const tstring &filePath);

    // checks if the specified directory is not empty
    // returns true if the path is an existing directory and
    // it contains at least one file other than "." or "..".
    bool isDirectoryNotEmpty(const tstring &dirPath);

} // FileUtils

#endif // FILEUTILS_H
