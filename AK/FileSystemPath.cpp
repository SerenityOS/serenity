#include "FileSystemPath.h"
#include "Vector.h"
#include "kstdio.h"
#include "StringBuilder.h"

namespace AK {

FileSystemPath::FileSystemPath(const String& s)
    : m_string(s)
{
    m_is_valid = canonicalize();
}

bool FileSystemPath::canonicalize(bool resolve_symbolic_links)
{
    // FIXME: Implement "resolveSymbolicLinks"
    (void) resolve_symbolic_links;
    auto parts = m_string.split('/');
    Vector<String> canonical_parts;

    for (auto& part : parts) {
        if (part == ".")
            continue;
        if (part == "..") {
            if (!canonical_parts.isEmpty())
                canonical_parts.takeLast();
            continue;
        }
        if (!part.isEmpty())
            canonical_parts.append(part);
    }
    if (canonical_parts.isEmpty()) {
        m_string = m_basename = "/";
        return true;
    }

    m_basename = canonical_parts.last();
    StringBuilder builder;
    for (auto& cpart : canonical_parts) {
        builder.append('/');
        builder.append(move(cpart));
    }
    m_string = builder.build();
    return true;
}

}

