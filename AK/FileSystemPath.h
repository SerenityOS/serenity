#pragma once

#include "AKString.h"

namespace AK {

class FileSystemPath {
public:
    FileSystemPath() {}
    explicit FileSystemPath(const StringView&);

    bool is_valid() const { return m_is_valid; }
    const String& string() const { return m_string; }

    const String& basename() const { return m_basename; }

    const Vector<String>& parts() const { return m_parts; }

    bool has_extension(StringView) const;

private:
    void canonicalize();

    Vector<String> m_parts;
    String m_string;
    String m_basename;
    bool m_is_valid { false };
};

String canonicalized_path(const StringView&);

};

using AK::FileSystemPath;
using AK::canonicalized_path;
