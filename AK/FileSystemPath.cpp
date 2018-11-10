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
        m_string = "/";
        return true;
    }
    StringBuilder builder;
    for (auto& cpart : canonicalParts) {
        builder.append('/');
        builder.append(move(cpart));
    }
    m_string = builder.build();
    return true;
}

}

