#pragma once

#include "String.h"

namespace AK {

class FileSystemPath {
public:
    FileSystemPath() { }
    explicit FileSystemPath(const String&);

    bool isValid() const { return m_isValid; }
    String string() const { return m_string; }

    String basename() const { return m_basename; }

private:
    bool canonicalize(bool resolveSymbolicLinks = false);

    String m_string;
    String m_basename;
    bool m_isValid { false };
};

};

using AK::FileSystemPath;
