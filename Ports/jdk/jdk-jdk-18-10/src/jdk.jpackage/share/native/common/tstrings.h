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

#ifndef TSTRINGS_H
#define TSTRINGS_H

#ifdef _MSC_VER
#   define TSTRINGS_WITH_WCHAR
#endif

#ifdef TSTRINGS_WITH_WCHAR
#include <windows.h>
#include <tchar.h>
// Want compiler issue C4995 warnings for encounters of deprecated functions.
#include <strsafe.h>
#endif

// STL's string header depends on deprecated functions.
// We don't care about warnings from STL header, so disable them locally.
#ifdef _MSC_VER
#   pragma warning(push)
#   pragma warning(disable:4995)
#endif

#include <string>
#include <sstream>
#include <iostream>
#include <vector>

#ifdef _MSC_VER
#   pragma warning(pop)
#endif


#ifndef _T
#   define _T(x) x
#endif


#ifdef TSTRINGS_WITH_WCHAR
typedef std::wstring        tstring;
typedef std::wostringstream tostringstream;
typedef std::wistringstream tistringstream;
typedef std::wstringstream  tstringstream;
typedef std::wistream       tistream;
typedef std::wostream       tostream;
typedef std::wiostream      tiostream;
typedef std::wios           tios;
#else
typedef std::string         tstring;
typedef std::ostringstream  tostringstream;
typedef std::istringstream  tistringstream;
typedef std::stringstream   tstringstream;
typedef std::istream        tistream;
typedef std::ostream        tostream;
typedef std::iostream       tiostream;
typedef std::ios            tios;

typedef const char* LPCTSTR;
typedef char TCHAR;
#endif

// frequently used "array of tstrings" type
typedef std::vector<tstring> tstring_array;

namespace tstrings {
    tstring unsafe_format(tstring::const_pointer format, ...);

    enum CompareType {CASE_SENSITIVE, IGNORE_CASE};
    bool equals(const tstring& a, const tstring& b,
            const CompareType ct=CASE_SENSITIVE);
    bool startsWith(const tstring &str, const tstring &substr,
            const CompareType ct=CASE_SENSITIVE);
    bool endsWith(const tstring &str, const tstring &substr,
            const CompareType ct=CASE_SENSITIVE);

    enum SplitType {ST_ALL, ST_EXCEPT_EMPTY_STRING};
    void split(tstring_array &strVector, const tstring &str,
            const tstring &delimiter, const SplitType st = ST_ALL);
    inline tstring_array split(const tstring &str, const tstring &delimiter,
            const SplitType st = ST_ALL) {
        tstring_array result;
        split(result, str, delimiter, st);
        return result;
    }
    tstring trim(const tstring& str, const tstring& whitespace = _T(" \t"));

    /**
     * Writes sequence of values from [b, e) range into string buffer inserting
     * 'delimiter' after each value except of the last one.
     * Returns contents of string buffer.
     */
    template <class It>
    tstring join(It b, It e, const tstring& delimiter=tstring()) {
        tostringstream buf;
        if (b != e) {
            for (;;) {
                buf << *b;
                if (++b == e) {
                    break;
                }
                buf << delimiter;
            }
        }
        return buf.str();
    }

    tstring toLower(const tstring& str);

    tstring replace(const tstring &str, const tstring &search,
            const tstring &replace);
}


namespace tstrings {
    inline std::string toUtf8(const std::string& utf8str) {
        return utf8str;
    }

#ifdef TSTRINGS_WITH_WCHAR
    // conversion to the active code page
    std::string toACP(const std::wstring& utf16str);

    // conversion from windows-encoding string (WIDECHAR or ACP) to utf8/utf16
    std::string winStringToUtf8(const std::wstring& winStr);
    std::string winStringToUtf8(const std::string& winStr);
    std::wstring winStringToUtf16(const std::wstring& winStr);
    std::wstring winStringToUtf16(const std::string& winStr);

    // conversion from utf8/utf16 to windows-encoding string (WIDECHAR or ACP)
    tstring toWinString(const std::wstring& utf16);
    tstring toWinString(const std::string& utf8);

    // conversion to Utf8
    std::string toUtf8(const std::wstring& utf16str);

    // conversion to Utf16
    std::wstring toUtf16(const std::string& utf8str);

    inline std::wstring fromUtf8(const std::string& utf8str) {
        return toUtf16(utf8str);
    }

    inline tstring fromUtf16(const std::wstring& utf16str) {
        return utf16str;
    }

#else
    inline std::string fromUtf8(const std::string& utf8str) {
        return utf8str;
    }
#endif
} // namespace tstrings


namespace tstrings {
namespace format_detail {

    template <class T>
    struct str_arg_value {
        const tstring value;

        str_arg_value(const std::string& v): value(fromUtf8(v)) {
        }

#ifdef TSTRINGS_WITH_WCHAR
        str_arg_value(const std::wstring& v): value(v) {
        }
#endif

        tstring::const_pointer operator () () const {
            return value.c_str();
        }
    };

    template <>
    struct str_arg_value<tstring> {
        const tstring::const_pointer value;

        str_arg_value(const tstring& v): value(v.c_str()) {
        }

        str_arg_value(tstring::const_pointer v): value(v) {
        }

        tstring::const_pointer operator () () const {
            return value;
        }
    };

    inline str_arg_value<std::string> arg(const std::string& v) {
        return v;
    }

    inline str_arg_value<std::string> arg(std::string::const_pointer v) {
        return std::string(v ? v: "(null)");
    }

#ifdef TSTRINGS_WITH_WCHAR
    inline str_arg_value<std::wstring> arg(const std::wstring& v) {
        return v;
    }

    inline str_arg_value<std::wstring> arg(std::wstring::const_pointer v) {
        return std::wstring(v ? v : L"(null)");
    }
#else
    void arg(const std::wstring&);          // Compilation error by design.
    void arg(std::wstring::const_pointer);  // Compilation error by design.
#endif

    template <class T>
    struct arg_value {
        arg_value(const T v): v(v) {
        }
        T operator () () const {
            return v;
        }
    private:
        const T v;
    };

    inline arg_value<int> arg(int v) {
        return v;
    }
    inline arg_value<unsigned> arg(unsigned v) {
        return v;
    }
    inline arg_value<long> arg(long v) {
        return v;
    }
    inline arg_value<unsigned long> arg(unsigned long v) {
        return v;
    }
    inline arg_value<long long> arg(long long v) {
        return v;
    }
    inline arg_value<unsigned long long> arg(unsigned long long v) {
        return v;
    }
    inline arg_value<float> arg(float v) {
        return v;
    }
    inline arg_value<double> arg(double v) {
        return v;
    }
    inline arg_value<bool> arg(bool v) {
        return v;
    }
    inline arg_value<const void*> arg(const void* v) {
        return v;
    }

} // namespace format_detail
} // namespace tstrings


namespace tstrings {
    template <class T, class T2, class T3, class T4, class T5, class T6, class T7>
    inline tstring format(const tstring& fmt, const T& v, const T2& v2, const T3& v3, const T4& v4, const T5& v5, const T6& v6, const T7& v7) {
        return unsafe_format(fmt.c_str(),   format_detail::arg(v)(),
                                            format_detail::arg(v2)(),
                                            format_detail::arg(v3)(),
                                            format_detail::arg(v4)(),
                                            format_detail::arg(v5)(),
                                            format_detail::arg(v6)(),
                                            format_detail::arg(v7)());
    }

    template <class T, class T2, class T3, class T4, class T5, class T6>
    inline tstring format(const tstring& fmt, const T& v, const T2& v2, const T3& v3, const T4& v4, const T5& v5, const T6& v6) {
        return unsafe_format(fmt.c_str(),   format_detail::arg(v)(),
                                            format_detail::arg(v2)(),
                                            format_detail::arg(v3)(),
                                            format_detail::arg(v4)(),
                                            format_detail::arg(v5)(),
                                            format_detail::arg(v6)());
    }

    template <class T, class T2, class T3, class T4, class T5>
    inline tstring format(const tstring& fmt, const T& v, const T2& v2, const T3& v3, const T4& v4, const T5& v5) {
        return unsafe_format(fmt.c_str(),   format_detail::arg(v)(),
                                            format_detail::arg(v2)(),
                                            format_detail::arg(v3)(),
                                            format_detail::arg(v4)(),
                                            format_detail::arg(v5)());
    }

    template <class T, class T2, class T3, class T4>
    inline tstring format(const tstring& fmt, const T& v, const T2& v2, const T3& v3, const T4& v4) {
        return unsafe_format(fmt.c_str(),   format_detail::arg(v)(),
                                            format_detail::arg(v2)(),
                                            format_detail::arg(v3)(),
                                            format_detail::arg(v4)());
    }

    template <class T, class T2, class T3>
    inline tstring format(const tstring& fmt, const T& v, const T2& v2, const T3& v3) {
        return unsafe_format(fmt.c_str(),   format_detail::arg(v)(),
                                            format_detail::arg(v2)(),
                                            format_detail::arg(v3)());
    }

    template <class T, class T2>
    inline tstring format(const tstring& fmt, const T& v, const T2& v2) {
        return unsafe_format(fmt.c_str(),   format_detail::arg(v)(),
                                            format_detail::arg(v2)());

    }

    template <class T>
    inline tstring format(const tstring& fmt, const T& v) {
        return unsafe_format(fmt.c_str(),   format_detail::arg(v)());
    }
} // namespace tstrings


namespace tstrings {
    /**
     * Buffer that accepts both std::wstring and std::string instances doing
     * encoding conversions behind the scenes. All std::string-s assumed to be
     * UTF8-encoded, all std::wstring-s assumed to be UTF16-encoded.
     */
    class any {
    public:
        any() {
        }

        any(std::string::const_pointer msg) {
            data << fromUtf8(msg);
        }

        any(const std::string& msg) {
            data << fromUtf8(msg);
        }

#ifdef TSTRINGS_WITH_WCHAR
        any(std::wstring::const_pointer msg) {
            data << msg;
        }

        any(const std::wstring& msg) {
            data << msg;
        }

        any& operator << (const std::wstring& v) {
            data << v;
            return *this;
        }

        // need this specialization instead std::wstring::pointer,
        // otherwise LPWSTR is handled as abstract pointer (void*)
        any& operator << (LPWSTR v) {
            data << (v ? v : L"NULL");
            return *this;
        }

        // need this specialization instead std::wstring::const_pointer,
        // otherwise LPCWSTR is handled as abstract pointer (const void*)
        any& operator << (LPCWSTR v) {
            data << (v ? v : L"NULL");
            return *this;
        }

        std::wstring wstr() const {
            return data.str();
        }
#endif

        template <class T>
        any& operator << (T v) {
            data << v;
            return *this;
        }

        any& operator << (tostream& (*pf)(tostream&)) {
            data << pf;
            return *this;
        }

        any& operator << (tios& (*pf)(tios&)) {
            data << pf;
            return *this;
        }

        any& operator << (std::ios_base& (*pf)(std::ios_base&)) {
            data << pf;
            return *this;
        }

        std::string str() const {
            return toUtf8(data.str());
        }

        tstring tstr() const {
            return data.str();
        }

    private:
        tostringstream data;
    };

    inline tstring to_tstring(const any& val) {
        return val.tstr();
    }
} // namespace tstrings


inline std::ostream& operator << (std::ostream& os, const tstrings::any& buf) {
    os << buf.str();
    return os;
}

#ifdef TSTRINGS_WITH_WCHAR
inline std::wostream& operator << (std::wostream& os, const tstrings::any& buf) {
    os << buf.wstr();
    return os;
}
#endif

#endif //TSTRINGS_H
