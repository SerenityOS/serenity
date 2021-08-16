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

#include <sstream>
#include <iomanip>
#include "VersionInfo.h"
#include "ResourceEditor.h"
#include "ErrorHandling.h"
#include "Toolbox.h"


VersionInfo::VersionInfo() {

}

namespace {

class FixedFileVersion {
public:
    FixedFileVersion(const std::wstring& value) {
        if (4 != swscanf_s(value.c_str(), L"%d.%d.%d.%d", components + 0,
                components + 1, components + 2, components + 3)) {
            JP_THROW(tstrings::any()
                    << "Malformed file version value: ["
                    << value
                    << "]");
            forEach(components, [&value](int component) -> void {
                if (USHRT_MAX < component) {
                    JP_THROW(tstrings::any()
                        << "Invalid file version value: ["
                        << value
                        << "]");
                }
            });
        }
    }

    void apply(DWORD& ms, DWORD& ls) const {
        ms = MAKELONG(components[1], components[0]);
        ls = MAKELONG(components[3], components[2]);
    }

private:
    int components[4];
};


std::ostream& writeWORD(std::ostream& cout, size_t v) {
    if (USHRT_MAX < v) {
        JP_THROW("Invalid WORD value");
    }
    return cout.write(reinterpret_cast<const char*>(&v), sizeof(WORD));
}


std::ostream& writeDWORD(std::ostream& cout, size_t v) {
    if (UINT_MAX < v) {
        JP_THROW("Invalid DWORD value");
    }

    return cout.write(reinterpret_cast<const char*>(&v), sizeof(DWORD));
}


std::ostream& write(std::ostream& cout, const VS_FIXEDFILEINFO& v) {
    return cout.write(reinterpret_cast<const char*>(&v), sizeof(v));
}

std::ostream& write(std::ostream& cout, const std::wstring& s) {
    return cout.write(reinterpret_cast<const char*>(s.c_str()),
            (s.size() + 1 /* trailing 0 */) * sizeof(wchar_t));
}

void add32bitPadding(std::ostream& cout) {
    enum { WordAlign = 2 };
    const std::streampos pos = cout.tellp();
    if (pos % 2) {
        JP_THROW("Invalid data written in the stream");
    }
    const int padding = WordAlign - (pos / 2) % WordAlign;
    if (WordAlign != padding) {
        for (int i = 0; i < padding; ++i) {
            writeWORD(cout, 0);
        }
    }
}


class StreamSize {
public:
    StreamSize(std::ostream& out): stream(out), anchor(out.tellp()) {
        writeWORD(stream, 0); // placeholder
    }

    ~StreamSize() {
        JP_TRY;

        const std::streampos curPos = stream.tellp();
        const std::streampos size = curPos - anchor;
        stream.seekp(anchor);
        if (size < 0) {
            JP_THROW("Invalid negative size value");
        }
        writeWORD(stream, (size_t) size);
        stream.seekp(curPos);

        JP_CATCH_ALL;
    }

private:
    std::ostream& stream;
    std::streampos anchor;
};

} // namespace

VersionInfo& VersionInfo::setProperty(
        const std::wstring& id, const std::wstring& value) {
    props[id] = value;

    if (id == L"FIXEDFILEINFO_FileVersion") {
        // Validate input
        const ::FixedFileVersion validate(value);
    }
    return *this;
}


const VersionInfo& VersionInfo::apply(
        const ResourceEditor::FileLock& fileLock) const {
    if (props.find(L"FIXEDFILEINFO_FileVersion") == props.end()) {
        JP_THROW("Missing mandatory FILEVERSION property");
    }

    std::stringstream buf(
            std::stringstream::in | std::stringstream::out | std::stringstream::binary);
    buf.exceptions(std::ios::failbit | std::ios::badbit);

    fillBuffer(buf);

    buf.seekg(0);

    ResourceEditor()
        .id(MAKEINTRESOURCE(VS_VERSION_INFO))
        .type(RT_VERSION)
        .apply(fileLock, buf);
    return *this;
}


void VersionInfo::fillBuffer(std::ostream& buf) const {
    // Fill VS_VERSIONINFO pseudo structure
    StreamSize versionInfoLength(buf); // wLength
    writeWORD(buf, sizeof(VS_FIXEDFILEINFO)); // wValueLength
    writeWORD(buf, 0); // wType
    write(buf, L"VS_VERSION_INFO"); // szKey
    add32bitPadding(buf); // Padding1
    write(buf, createFIXEDFILEINFO()); // Value
    add32bitPadding(buf); // Padding2

    const DWORD neutralLangId = (0x04b0 | MAKELANGID(LANG_NEUTRAL, SUBLANG_NEUTRAL) << 16);
    const DWORD engLangId = (0x04b0 | MAKELANGID(LANG_ENGLISH, SUBLANG_ENGLISH_US) << 16);

    do {
        // Fill StringFileInfo pseudo structure
        StreamSize stringFileInfoLength(buf); // wLength
        writeWORD(buf, 0); // wValueLength
        writeWORD(buf, 1); // wType
        write(buf, L"StringFileInfo"); // szKey
        add32bitPadding(buf); // Padding

        // Fill StringTable pseudo structure
        StreamSize stringTableLength(buf); // wLength
        writeWORD(buf, 0); // wValueLength
        writeWORD(buf, 1); // wType

        std::wstringstream strLangIdBuf;
        strLangIdBuf
            << std::uppercase
            << std::hex
            << std::setw(8)
            << std::setfill(L'0')
            << engLangId;
        const std::wstring strLangId = strLangIdBuf.str();
        write(buf, strLangId); // szKey
        add32bitPadding(buf); // Padding

        forEach(props, [&buf](const PropertyMap::value_type& entry) -> void {
            if (entry.first.rfind(L"FIXEDFILEINFO_", 0) == 0) {
                // Ignore properties to be used to initialize data in
                // VS_FIXEDFILEINFO structure.
                return;
            }

            // Fill String pseudo structure
            StreamSize stringLength(buf); // wLength
            writeWORD(buf, entry.second.size()); // wValueLength
            writeWORD(buf, 1); // wType
            write(buf, entry.first); // wKey
            add32bitPadding(buf); // Padding1
            write(buf, entry.second); // Value
            add32bitPadding(buf); // Padding2
        });
    } while (0);

    // Fill VarFileInfo pseudo structure
    StreamSize varFileInfoLength(buf); // wLength
    writeWORD(buf, 0); // wValueLength
    writeWORD(buf, 1); // wType
    write(buf, L"VarFileInfo"); // szKey
    add32bitPadding(buf); // Padding

    // Fill Var pseudo structure
    StreamSize varLength(buf); // wLength
    writeWORD(buf, sizeof(DWORD)); // wValueLength
    writeWORD(buf, 0); // wType
    write(buf, L"Translation"); // szKey
    add32bitPadding(buf); // Padding
    writeDWORD(buf, MAKELANGID(LANG_NEUTRAL, SUBLANG_NEUTRAL)); // Value
}


VS_FIXEDFILEINFO VersionInfo::createFIXEDFILEINFO() const {
    const ::FixedFileVersion fileVersion(props.find(
            L"FIXEDFILEINFO_FileVersion")->second);

    VS_FIXEDFILEINFO result;
    ZeroMemory(&result, sizeof(result));

    result.dwSignature = 0xFEEF04BD;
    result.dwStrucVersion = 0x00010000;
    result.dwFileOS = VOS_NT_WINDOWS32;
    result.dwFileType = VFT_APP;

    fileVersion.apply(result.dwFileVersionMS, result.dwFileVersionLS);

    PropertyMap::const_iterator entry = props.find(
            L"FIXEDFILEINFO_ProductVersion");
    if (entry == props.end()) {
        fileVersion.apply(result.dwProductVersionMS, result.dwProductVersionLS);
    } else {
        bool fatalError = false;
        try {
            const ::FixedFileVersion productVersion(entry->second);
            fatalError = true;
            productVersion.apply(result.dwProductVersionMS,
                    result.dwProductVersionLS);
        } catch (const std::exception&) {
            if (fatalError) {
                throw;
            }
            // Failed to parse product version as four component version string.
            fileVersion.apply(result.dwProductVersionMS,
                    result.dwProductVersionLS);
        }
    }

    return result;
}
