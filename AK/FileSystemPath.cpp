#include "FileSystemPath.h"
#include "Vector.h"
#include "kstdio.h"
#include "StringBuilder.h"

namespace AK {

FileSystemPath::FileSystemPath(const String& s)
    : m_string(s)
{
    m_isValid = canonicalize();
}

bool FileSystemPath::canonicalize(bool resolveSymbolicLinks)
{
    // FIXME: Implement "resolveSymbolicLinks"
    (void) resolveSymbolicLinks;
    auto parts = m_string.split('/');
    Vector<String> canonicalParts;

    for (auto& part : parts) {
        if (part == ".")
            continue;
        if (part == "..") {
            if (!canonicalParts.isEmpty())
                canonicalParts.takeLast();
            continue;
        }
        if (!part.isEmpty())
            canonicalParts.append(part);
    }
    if (canonicalParts.isEmpty()) {
        m_string = m_basename = m_dirname = "/";
        return true;
    }

    m_basename = canonicalParts.last();

    if (canonicalParts.size() == 1) {
        m_dirname = "/";
    } else {
        StringBuilder builder;
        for (size_t i = 0; i < canonicalParts.size() - 1; ++i) {
            auto& cpart = canonicalParts[i];
            builder.append('/');
            builder.append(cpart);
        }
        m_dirname = builder.build();
    }

    {
        StringBuilder builder;
        for (auto& cpart : canonicalParts) {
            builder.append('/');
            builder.append(move(cpart));
        }
        m_string = builder.build();
    }
    return true;
}

}

