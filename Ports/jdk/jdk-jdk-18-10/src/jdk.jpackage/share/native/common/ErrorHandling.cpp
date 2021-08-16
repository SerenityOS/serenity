/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <algorithm>
#include <string.h>
#include <cerrno>

#include "ErrorHandling.h"
#include "Log.h"


namespace {

tstring getFilename(const SourceCodePos& pos) {
    const std::string buf(pos.file);
    const std::string::size_type idx = buf.find_last_of("\\/");
    if (idx == std::string::npos) {
        return tstrings::fromUtf8(buf);
    }
    return tstrings::fromUtf8(buf.substr(idx + 1));
}

void reportError(const SourceCodePos& pos, const tstring& msg) {
    Logger::defaultLogger().log(Logger::LOG_ERROR, getFilename(pos).c_str(),
        pos.lno, tstrings::fromUtf8(pos.func).c_str(), msg);
}

} // namespace

void reportError(const SourceCodePos& pos, const std::runtime_error& e) {
    reportError(pos, (tstrings::any() << "Exception with message \'"
                                        << e.what() << "\' caught").tstr());
}


void reportUnknownError(const SourceCodePos& pos) {
    reportError(pos, _T("Unknown exception caught"));
}


std::string makeMessage(const std::runtime_error& e, const SourceCodePos& pos) {
    std::ostringstream printer;
    printer << getFilename(pos) << "(" << pos.lno << ") at "
            << pos.func << "(): "
            << e.what();
    return printer.str();
}


namespace {

bool isNotSpace(int chr) {
    return isspace(chr) == 0;
}


enum TrimMode {
    TrimLeading = 0x10,
    TrimTrailing = 0x20,
    TrimBoth = TrimLeading | TrimTrailing
};

// Returns position of the last printed character in the given string.
// Returns std::string::npos if nothing was printed.
size_t printWithoutWhitespaces(std::ostream& out, const std::string& str,
                                                            TrimMode mode) {
    std::string::const_reverse_iterator it = str.rbegin();
    std::string::const_reverse_iterator end = str.rend();

    if (mode & TrimLeading) {
        // skip leading whitespace
        std::string::const_iterator entry = std::find_if(str.begin(),
                                                str.end(), isNotSpace);
        end = std::string::const_reverse_iterator(entry);
    }

    if (mode & TrimTrailing) {
        // skip trailing whitespace
        it = std::find_if(it, end, isNotSpace);
    }

    if (it == end) {
        return std::string::npos;
    }

    const size_t pos = str.rend() - end;
    const size_t len = end - it;
    out.write(str.c_str() + pos, len);
    return pos + len - 1;
}

} // namespace

std::string joinErrorMessages(const std::string& a, const std::string& b) {
    const std::string endPhraseChars(";.,:!?");
    const std::string space(" ");
    const std::string dotAndSpace(". ");

    std::ostringstream printer;
    printer.exceptions(std::ios::failbit | std::ios::badbit);

    size_t idx = printWithoutWhitespaces(printer, a, TrimTrailing);
    size_t extra = 0;
    if (idx < a.size() && endPhraseChars.find(a[idx]) == std::string::npos) {
        printer << dotAndSpace;
        extra = dotAndSpace.size();
    } else if (idx != std::string::npos) {
        printer << space;
        extra = space.size();
    }

    idx = printWithoutWhitespaces(printer, b, TrimBoth);

    const std::string str = printer.str();

    if (std::string::npos == idx && extra) {
        // Nothing printed from the 'b' message. Backout delimiter string.
        return str.substr(0, str.size() - extra);
    }
    return str;
}


std::string lastCRTError() {
#ifndef _WIN32
    return strerror(errno);
#else
    TCHAR buffer[2048];
    if (0 == _tcserror_s(buffer, errno)) {
        return (tstrings::any() << buffer).str();
    }
    return "";
#endif
}
