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

#ifndef Guid_h
#define Guid_h

#include <windows.h>
#include "tstrings.h"


class Guid {
public:
    Guid(const std::string& str);
    Guid(const std::wstring& str);
    Guid(const GUID& v);
    Guid();

    // Comparison for equality is the only comparison operation that make
    // sense for GUIDs. However in order to use STL algorithms with
    // Guid class need to define less operator.
    bool operator < (const Guid& other) const;
    bool operator == (const Guid& other) const;
    bool operator != (const Guid& other) const {
        return ! (*this == other);
    }

    enum StringifyFlags {
        WithCurlyBrackets = 0x0,
        WithDashes = 0x0,
        UpperCase = 0x0,
        StringifyDefaults = WithCurlyBrackets | UpperCase | WithDashes,
        NoCurlyBrackets = 0x1,
        NoDashes = 0x2,
        LowerCase = 0x4,
    };

    tstring toString(int flags=StringifyDefaults) const;

    /**
     * Returns string GUID representation of this instance compatible with
     * Windows MSI API.
     */
    tstring toMsiString() const {
        return toString(UpperCase | WithCurlyBrackets | WithDashes);
    }

    static Guid generate();

private:
    GUID value;
};

#endif // #ifndef Guid_h
