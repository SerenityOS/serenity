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


#ifndef CfgFile_h
#define CfgFile_h

#include <map>
#include "tstrings.h"


class CfgFile {
public:
    template <class Tag> class Id {
    public:
        Id(const tstring::const_pointer str) : str(str) {

        }

        bool operator == (const Id& other) const {
            return tstring(str) == tstring(other.str);
        }

        bool operator != (const Id& other) const {
            return !operator == (other);
        }

        bool operator < (const Id& other) const {
            return tstring(str) < tstring(other.str);
        }

        tstring name() const {
            return tstring(str);
        }

    private:
        tstring::const_pointer str;
    };

    class PropertyNameTag {};
    typedef Id<PropertyNameTag> PropertyName;

    class SectionNameTag {};
    typedef Id<SectionNameTag> SectionName;

    typedef std::map<PropertyName, tstring_array> Properties;

    /**
     * Returns properties of the given section.
     */
    const Properties& getProperties(const SectionName& sectionName) const;

    /**
     * Sets new value of the given property in the given section.
     */
    CfgFile& setPropertyValue(const SectionName& sectionName,
        const PropertyName& name, const tstring& value) {
        tstring_array ctnr;
        ctnr.push_back(value);
        return setPropertyValue(sectionName, name, ctnr);
    }

    CfgFile& setPropertyValue(const SectionName& sectionName,
        const PropertyName& name, const tstring_array& value);

    typedef std::map<tstring, tstring> Macros;

    /**
     * Returns copy of this instance with the given macros expanded.
     */
    CfgFile expandMacros(const Macros& macros) const;

    static CfgFile load(const tstring& path);

    static tstring asString(Properties::const_reference property);

    static tstring asPathList(Properties::const_reference property);

    typedef std::map<SectionName, Properties> PropertyMap;

private:
    PropertyMap data;
    Properties empty;
};


namespace SectionName {
    extern const CfgFile::SectionName Application;
    extern const CfgFile::SectionName JavaOptions;
    extern const CfgFile::SectionName AppCDSJavaOptions;
    extern const CfgFile::SectionName AppCDSGenerateCacheJavaOptions;
    extern const CfgFile::SectionName ArgOptions;
} // namespace SectionName

namespace PropertyName {
    extern const CfgFile::PropertyName version;
    extern const CfgFile::PropertyName mainjar;
    extern const CfgFile::PropertyName mainmodule;
    extern const CfgFile::PropertyName mainclass;
    extern const CfgFile::PropertyName classpath;
    extern const CfgFile::PropertyName modulepath;
    extern const CfgFile::PropertyName runtime;
    extern const CfgFile::PropertyName splash;
    extern const CfgFile::PropertyName memory;
    extern const CfgFile::PropertyName arguments;
    extern const CfgFile::PropertyName javaOptions;
} // namespace AppPropertyName

#endif // CfgFile_h
