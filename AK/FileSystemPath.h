#pragma once

#include "AKString.h"

namespace AK {

class FileSystemPath {
public:
    FileSystemPath() { }
    explicit FileSystemPath(const String&);

    bool is_valid() const { return m_is_valid; }
    String string() const { return m_string; }

    String basename() const { return m_basename; }

    const Vector<String>& parts() const { return m_parts; }

    bool has_extension(StringView) const;

private:
    bool canonicalize(bool resolve_symbolic_links = false);

    Vector<String> m_parts;
    String m_string;
    String m_basename;
    bool m_is_valid { false };
};

};

using AK::FileSystemPath;
