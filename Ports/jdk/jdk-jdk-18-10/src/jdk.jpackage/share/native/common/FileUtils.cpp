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

#include <memory>
#include <algorithm>

#include "FileUtils.h"

namespace FileUtils {

#ifdef _WIN32
const tstring::value_type pathSeparator = _T(';');
#else
const tstring::value_type pathSeparator = _T(':');
#endif

namespace {
#ifdef _WIN32
    const tstring::value_type dirSeparator = _T('\\');
    const tstring::value_type alianDirSeparator = _T('/');
#else
    const tstring::value_type dirSeparator = _T('/');
    const tstring::value_type alianDirSeparator = _T('\\');
#endif
} // namespace


bool isDirSeparator(const tstring::value_type c) {
    return (c == dirSeparator || c == alianDirSeparator);
}


tstring dirname(const tstring &path) {
    tstring::size_type pos;
    if (tstrings::endsWith(path, _T("/.")) || tstrings::endsWith(path, _T("\\."))) {
        // this method is really getparent dirname - if the path ends with "/.",
        // we need to ignore that when looking for the last "/" to find parent
        pos = (path.substr(0, path.length() - 2)).find_last_of(_T("\\/"));
    } else {
        pos = path.find_last_of(_T("\\/"));
    }

    if (pos != tstring::npos) {
        pos = path.find_last_not_of(_T("\\/"), pos); // skip trailing slashes
    }
    return pos == tstring::npos ? tstring() : path.substr(0, pos + 1);
}


tstring basename(const tstring &path) {
    const tstring::size_type pos = path.find_last_of(_T("\\/"));
    if (pos == tstring::npos) {
        return path;
    }
    return path.substr(pos + 1);
}


tstring suffix(const tstring &path) {
    const tstring::size_type pos = path.rfind(_T('.'));
    if (pos == tstring::npos) {
        return tstring();
    }
    const tstring::size_type dirSepPos = path.find_first_of(_T("\\/"),
                                                            pos + 1);
    if (dirSepPos != tstring::npos) {
        return tstring();
    }
    // test for '/..' and '..' cases
    if (pos != 0 && path[pos - 1] == _T('.')
                            && (pos == 1 || isDirSeparator(path[pos - 2]))) {
        return tstring();
    }
    return path.substr(pos);
}


tstring combinePath(const tstring& parent, const tstring& child) {
    if (parent.empty()) {
        return child;
    }
    if (child.empty()) {
        return parent;
    }

    tstring parentWOSlash = removeTrailingSlash(parent);
    // also handle the case when child contains starting slash
    bool childHasSlash = isDirSeparator(*child.begin());
    tstring childWOSlash = childHasSlash ? child.substr(1) : child;

    return parentWOSlash.append(1, dirSeparator).append(childWOSlash);
}


tstring removeTrailingSlash(const tstring& path) {
    if (path.empty()) {
        return path;
    }
    tstring::const_reverse_iterator it = path.rbegin();
    tstring::const_reverse_iterator end = path.rend();

    while (it != end && isDirSeparator(*it)) {
        ++it;
    }
    return path.substr(0, end - it);
}


tstring normalizePath(tstring v) {
    std::replace(v.begin(), v.end(), alianDirSeparator, dirSeparator);
#ifdef _WIN32
    return tstrings::toLower(v);
#else
    return v;
#endif
}


tstring replaceSuffix(const tstring& path, const tstring& newSuffix) {
    const tstring oldSuffix = suffix(path);
    if (oldSuffix.empty()) {
        return tstring().append(path).append(newSuffix);
    }

    return path.substr(0, path.size() - oldSuffix.size()).append(newSuffix);
}

} //  namespace FileUtils
