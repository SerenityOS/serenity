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

#include <stdio.h>
#include <stdarg.h>
#include <stdexcept>
#include <algorithm>

#include "tstrings.h"
#include "ErrorHandling.h"


namespace tstrings {

/* Create formatted string
 */
tstring unsafe_format(tstring::const_pointer format, ...) {
    if (!format) {
        throw std::invalid_argument("Destination buffer can't be NULL");
    }

    tstring fmtout;
    int ret;
    const int inc = 256;

    va_list args;
    va_start(args, format);
    do {
        fmtout.resize(fmtout.size() + inc);
#ifdef _MSC_VER
        ret = _vsntprintf_s(&*fmtout.begin(), fmtout.size(), _TRUNCATE, format, args);
#else
#if defined(__GNUC__) && __GNUC__ >= 5
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wformat-nonliteral"
#endif
        // With g++ this compiles only with '-std=gnu++0x' option
        ret = vsnprintf(&*fmtout.begin(), fmtout.size(), format, args);
#if defined(__GNUC__) && __GNUC__ >= 5
#pragma GCC diagnostic pop
#endif
#endif
    } while(-1 == ret);
    va_end(args);

    //update string size by actual value
    fmtout.resize(ret);

    return fmtout;
}

/*
 * Tests if two strings are equal according to CompareType.
 *
 * a - string to compare
 * b - string to compare
 * ct - CASE_SENSITIVE: case sensitive comparing type
 *      IGNORE_CASE: case insensitive comparing type
 */
bool equals(const tstring& a, const tstring& b, const CompareType ct) {
    if (IGNORE_CASE==ct) {
        return toLower(a) == toLower(b);
    }
    return a == b;
}

bool startsWith(const tstring &str, const tstring &substr, const CompareType ct)
{
    if (str.size() < substr.size()) {
        return false;
    }
    const tstring startOfStr = str.substr(0, substr.size());
    return tstrings::equals(startOfStr, substr, ct);
}

bool endsWith(const tstring &str, const tstring &substr, const CompareType ct)
{
    if (str.size() < substr.size()) {
        return false;
    }
    const tstring endOfStr = str.substr(str.size() - substr.size());
    return tstrings::equals(endOfStr, substr, ct);
}

/*
 * Split string into a vector with given delimiter string
 *
 * strVector - string vector to store split tstring
 * str - string to split
 * delimiter - delimiter to split the string around
 * st - ST_ALL: return value includes an empty string
 *      ST_EXCEPT_EMPTY_STRING: return value does not include an empty string
 *
 * Note: It does not support multiple delimiters
 */
void split(tstring_array &strVector, const tstring &str,
          const tstring &delimiter, const SplitType st) {
    tstring::size_type start = 0, end = 0, length = str.length();

    if (length == 0 || delimiter.length() == 0) {
        return;
    }

    end = str.find(delimiter, start);
    while(end != tstring::npos) {
        if(st == ST_ALL || end - start > 1 ) {
            strVector.push_back(str.substr(start, end == tstring::npos ?
                                                  tstring::npos : end - start));
        }
        start = end > (tstring::npos - delimiter.size()) ?
                tstring::npos : end + delimiter.size();
        end = str.find(delimiter, start);
    }

    if(st == ST_ALL || start < length) {
        strVector.push_back(str.substr(start, length - start));
    }
}

/*
 * Convert uppercase letters to lowercase
 */
tstring toLower(const tstring& str) {
    tstring lower(str);
    tstring::iterator ok = std::transform(lower.begin(), lower.end(),
                                          lower.begin(), tolower);
    if (ok!=lower.end()) {
        lower.resize(0);
    }
    return lower;
}


/*
 * Replace all substring occurrences in a tstring.
 * If 'str' or 'search' is empty the function returns 'str'.
 * The given 'str' remains unchanged in any case.
 * The function returns changed copy of 'str'.
 */
tstring replace(const tstring &str, const tstring &search, const tstring &replace)
{
    if (search.empty()) {
        return str;
    }

    tstring s(str);

    for (size_t pos = 0; ; pos += replace.length()) {
        pos = s.find(search, pos);
        if (pos == tstring::npos) {
            break;
        }
        s.erase(pos, search.length());
        s.insert(pos, replace);
    }
    return s;
}


/*
 * Remove trailing spaces
 */

tstring trim(const tstring& str, const tstring& whitespace) {
    const size_t strBegin = str.find_first_not_of(whitespace);
    if (strBegin == std::string::npos) {
        return tstring(); // no content
    }

    const size_t  strEnd = str.find_last_not_of(whitespace);
    const size_t strRange = strEnd - strBegin + 1;

    return str.substr(strBegin, strRange);
}

} // namespace tstrings


#ifdef TSTRINGS_WITH_WCHAR
namespace tstrings {

namespace {
/*
 * Converts UTF16-encoded string into multi-byte string of the given encoding.
 */
std::string toMultiByte(const std::wstring& utf16str, int encoding) {
    std::string reply;
    int cm = WideCharToMultiByte(encoding,
                                 0,
                                 utf16str.c_str(),
                                 int(utf16str.size()),
                                 NULL,
                                 0,
                                 NULL,
                                 NULL);
    if (cm < 0) {
        JP_THROW("Unexpected reply from WideCharToMultiByte()");
    }
    if (0 == cm) {
        return reply;
    }

    reply.resize(cm);
    int cm2 = WideCharToMultiByte(encoding,
                                  0,
                                  utf16str.c_str(),
                                  int(utf16str.size()),
                                  &*reply.begin(),
                                  cm,
                                  NULL,
                                  NULL);
    if (cm != cm2) {
        JP_THROW("Unexpected reply from WideCharToMultiByte()");
    }

    return reply;
}

/*
 * Converts multi-byte string of the given encoding into UTF16-encoded string.
 */
std::wstring fromMultiByte(const std::string& str, int encoding) {
    std::wstring utf16;
    int cw = MultiByteToWideChar(encoding,
                                 MB_ERR_INVALID_CHARS,
                                 str.c_str(),
                                 int(str.size()),
                                 NULL,
                                 0);
    if (cw < 0) {
        JP_THROW("Unexpected reply from MultiByteToWideChar()");
    }
    if (0 == cw) {
        return utf16;
    }

    utf16.resize(cw);
    int cw2 = MultiByteToWideChar(encoding,
                                  MB_ERR_INVALID_CHARS,
                                  str.c_str(),
                                  int(str.size()),
                                  &*utf16.begin(),
                                  cw);
    if (cw != cw2) {
        JP_THROW("Unexpected reply from MultiByteToWideChar()");
    }

    return utf16;
}
} // namespace

std::string toACP(const std::wstring& utf16str) {
    return toMultiByte(utf16str, CP_ACP);
}

std::string toUtf8(const std::wstring& utf16str) {
    return toMultiByte(utf16str, CP_UTF8);
}

std::wstring toUtf16(const std::string& utf8str) {
    return fromMultiByte(utf8str, CP_UTF8);
}

// converts utf16-encoded string to Windows encoded string (WIDECHAR or ACP)
tstring toWinString(const std::wstring& utf16) {
#if defined(_UNICODE) || defined(UNICODE)
    return utf16;
#else
    return toMultiByte(utf16, CP_ACP);
#endif
}

// converts utf8-encoded string to Windows encoded string (WIDECHAR or ACP)
tstring toWinString(const std::string& utf8) {
    return toWinString(tstrings::toUtf16(utf8));
}


std::string winStringToUtf8(const std::wstring& winStr) {
    return toUtf8(winStr);
}

std::string winStringToUtf8(const std::string& winStr) {
    return toUtf8(fromMultiByte(winStr, CP_ACP));
}

std::wstring winStringToUtf16(const std::wstring& winStr) {
    return winStr;
}

std::wstring winStringToUtf16(const std::string& winStr) {
    return fromMultiByte(winStr, CP_ACP);
}

} // namespace tstrings
#endif // ifdef TSTRINGS_WITH_WCHAR
