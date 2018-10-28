#pragma once

#include "String.h"

namespace AK {

class FileSystemPath {
public:
    FileSystemPath() { }
    explicit FileSystemPath(const String&);

    bool isValid() const { return m_isValid; }
    String string() const { return m_string; }

private:
    bool canonicalize(bool resolveSymbolicLinks = false);

    String m_string;
    bool m_isValid { false };
};

};

using AK::FileSystemPath;
