#pragma once

#include <AK/String.h>

namespace AK {

class FileSystemPath {
public:
    FileSystemPath() {}
    explicit FileSystemPath(const StringView&);

    bool is_valid() const { return m_is_valid; }
    const String& string() const { return m_string; }

    const String& dirname() const { return m_dirname; }
    const String& basename() const { return m_basename; }
    const String& title() const { return m_title; }
    const String& extension() const { return m_extension; }

    const Vector<String>& parts() const { return m_parts; }

    bool has_extension(StringView) const;

private:
    void canonicalize();

    Vector<String> m_parts;
    String m_string;
    String m_dirname;
    String m_basename;
    String m_title;
    String m_extension;
    bool m_is_valid { false };
};

String canonicalized_path(const StringView&);

};

using AK::canonicalized_path;
using AK::FileSystemPath;
