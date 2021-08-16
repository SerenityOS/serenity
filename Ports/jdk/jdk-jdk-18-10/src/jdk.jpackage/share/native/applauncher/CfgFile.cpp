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

#include "kludge_c++11.h"

#include <fstream>
#include <algorithm>
#include "CfgFile.h"
#include "Log.h"
#include "Toolbox.h"
#include "FileUtils.h"
#include "ErrorHandling.h"


const CfgFile::Properties& CfgFile::getProperties(
    const SectionName& sectionName) const {
    const PropertyMap::const_iterator entry = data.find(sectionName);
    if (entry != data.end()) {
        return entry->second;
    }
    return empty;
}


CfgFile& CfgFile::setPropertyValue(const SectionName& sectionName,
    const PropertyName& name, const tstring_array& value) {
    PropertyMap::iterator entry = data.find(sectionName);
    if (entry != data.end()) {
        entry->second[name] = value;
    } else {
        Properties props;
        props[name] = value;
        data[sectionName] = props;
    }
    return *this;
}


namespace {

tstring expandMacros(const tstring& str, const CfgFile::Macros& macros) {
    tstring reply = str;
    CfgFile::Macros::const_iterator it = macros.begin();
    const CfgFile::Macros::const_iterator end = macros.end();
    for (; it != end; ++it) {
        reply = tstrings::replace(reply, it->first, it->second);
    }
    return reply;
}

} // namespace

CfgFile CfgFile::expandMacros(const Macros& macros) const {
    CfgFile copyCfgFile = *this;

    PropertyMap::iterator mapIt = copyCfgFile.data.begin();
    const PropertyMap::iterator mapEnd = copyCfgFile.data.end();
    for (; mapIt != mapEnd; ++mapIt) {
        Properties::iterator propertyIt = mapIt->second.begin();
        const Properties::iterator propertyEnd = mapIt->second.end();
        for (; propertyIt != propertyEnd; ++propertyIt) {
            tstring_array::iterator strIt = propertyIt->second.begin();
            const tstring_array::iterator strEnd = propertyIt->second.end();
            for (; strIt != strEnd; ++strIt) {
                tstring newValue;
                while ((newValue = ::expandMacros(*strIt, macros)) != *strIt) {
                    strIt->swap(newValue);
                }
            }
        }
    }

    return copyCfgFile;
}


namespace {

const CfgFile::SectionName* getSectionName(const tstring& str);
const CfgFile::PropertyName* getPropertyName(const tstring& str);

const CfgFile::SectionName UnknownSection = CfgFile::SectionName(_T(""));


class PurgeSection {
public:
    PurgeSection(CfgFile::SectionName& sectionName,
            CfgFile::Properties& sectionData,
            CfgFile::PropertyMap& cfgFileData):
                sectionName(sectionName), sectionData(sectionData),
                cfgFileData(cfgFileData) {
    }

    void operator ()() {
        if (sectionName != UnknownSection && !sectionData.empty()) {
            std::swap(cfgFileData[sectionName], sectionData);
            sectionName = UnknownSection;
            sectionData.clear();
        }
    }

private:
    CfgFile::SectionName& sectionName;
    CfgFile::Properties& sectionData;
    CfgFile::PropertyMap& cfgFileData;
};

class AddProperty {
public:
    AddProperty(const CfgFile::SectionName& sectionName,
            CfgFile::Properties& sectionData): sectionName(sectionName),
                sectionData(sectionData) {
    }

    void operator ()(const tstring& name, const tstring& value) {
        if (sectionName != UnknownSection) {
            const CfgFile::PropertyName *known = getPropertyName(name);
            if (known) {
                sectionData[*known].push_back(value);
            }
        }
    }

private:
    const CfgFile::SectionName& sectionName;
    CfgFile::Properties& sectionData;
};

} // namepsace

CfgFile CfgFile::load(const tstring& path) {
    std::ifstream input(path.c_str());
    if (!input.good()) {
        JP_THROW(tstrings::any() << "Error opening \"" << path << "\" file: "
                << lastCRTError());
    }

    CfgFile cfgFile;

    SectionName sectionName = UnknownSection;
    Properties sectionData;

    PurgeSection purgeSection(sectionName, sectionData, cfgFile.data);

    AddProperty addProperty(sectionName, sectionData);

    std::string utf8line;
    int lineno = 0;
    while (std::getline(input, utf8line)) {
        ++lineno;
        const tstring line = tstrings::any(utf8line).tstr();

        if (line.empty() || _T(';') == *line.begin()) {
            // Empty line or comment, ignore.
            continue;
        }

        if (_T('[') == *line.begin()) {
            const size_t endIdx = line.find_last_of(_T(']'));
            if (endIdx == tstring::npos) {
                JP_THROW(tstrings::any() << "Error parsing [" << path
                    << "] file at " << lineno << ": Missing ']' character");
            }

            purgeSection();

            // Section begin.
            const SectionName *knownName = getSectionName(line.substr(1, endIdx - 1));
            if (knownName) {
                sectionName = *knownName;
            } else {
                sectionName = UnknownSection;
            }
            continue;
        }

        size_t sepIdx = 0;
        do {
            sepIdx = line.find_first_of(_T('='), sepIdx);
            if (sepIdx == tstring::npos) {
                addProperty(line, tstring());
                break;
            }

            if (sepIdx != 0 && line[sepIdx - 1] == '\\') {
                sepIdx++;
                continue;
            }

            addProperty(line.substr(0, sepIdx), line.substr(sepIdx + 1));
            break;
        } while (true);
    }

    if (!input.eof()) {
        // Failed to process file up to the end.
        JP_THROW(tstrings::any() << "Failed to read \"" << path
                << "\" file up to the end: " << lastCRTError());
    }

    purgeSection();

    return cfgFile;
}


tstring join(const tstring_array& values, const tstring::value_type delimiter) {
    return tstrings::join(values.begin(), values.end(), tstring(1, delimiter));
}

tstring CfgFile::asString(Properties::const_reference property) {
    return *property.second.rbegin();
}

tstring CfgFile::asPathList(Properties::const_reference property) {
    return join(property.second, FileUtils::pathSeparator);
}


#define JP_ALL_SECTIONS \
    JP_SECTION(Application); \
    JP_SECTION(JavaOptions); \
    JP_SECTION(AppCDSJavaOptions); \
    JP_SECTION(AppCDSGenerateCacheJavaOptions); \
    JP_SECTION(ArgOptions);

namespace SectionName {
#define JP_SECTION(name) const CfgFile::SectionName name(_T(#name))
    JP_ALL_SECTIONS
#undef JP_SECTION
} // namespace SectionName

namespace {
    const CfgFile::SectionName* getSectionName(const tstring& str) {
#define JP_SECTION(name) while (str == _T(#name)) { return &SectionName::name; }
        JP_ALL_SECTIONS
#undef JP_SECTION
            return 0;
    }
}

#undef JP_ALL_SECTIONS


#define JP_ALL_PROPERTIES \
    JP_PROPERTY(version, "app.version"); \
    JP_PROPERTY(mainjar, "app.mainjar"); \
    JP_PROPERTY(mainmodule, "app.mainmodule"); \
    JP_PROPERTY(mainclass, "app.mainclass"); \
    JP_PROPERTY(classpath, "app.classpath"); \
    JP_PROPERTY(modulepath, "app.modulepath"); \
    JP_PROPERTY(runtime, "app.runtime"); \
    JP_PROPERTY(splash, "app.splash"); \
    JP_PROPERTY(memory, "app.memory"); \
    JP_PROPERTY(arguments, "arguments"); \
    JP_PROPERTY(javaOptions, "java-options"); \

namespace PropertyName {
#define JP_PROPERTY(varName, name) const CfgFile::PropertyName varName(_T(name))
    JP_ALL_PROPERTIES
#undef JP_PROPERTY
} // namespace PropertyName

namespace {
    const CfgFile::PropertyName* getPropertyName(const tstring& str) {
#define JP_PROPERTY(varName, name) while (str == _T(name)) { return &PropertyName::varName; }
        JP_ALL_PROPERTIES
#undef JP_PROPERTY
            return 0;
    }
} // namespace

#undef JP_ALL_PROPERTIES
