#include "FileSystemPath.h"
#include "StringBuilder.h"
#include "Vector.h"
#include "kstdio.h"

namespace AK {

FileSystemPath::FileSystemPath(const String& s)
    : m_string(s)
{
    m_is_valid = canonicalize();
}

bool FileSystemPath::canonicalize(bool resolve_symbolic_links)
{
    // FIXME: Implement "resolve_symbolic_links"
    (void)resolve_symbolic_links;
    auto parts = m_string.split('/');
    Vector<String> canonical_parts;

    for (auto& part : parts) {
        if (part == ".")
            continue;
        if (part == "..") {
            if (!canonical_parts.is_empty())
                canonical_parts.take_last();
            continue;
        }
        if (!part.is_empty())
            canonical_parts.append(part);
    }
    if (canonical_parts.is_empty()) {
        m_string = m_basename = "/";
        return true;
    }

    m_basename = canonical_parts.last();
    StringBuilder builder;
    for (auto& cpart : canonical_parts) {
        builder.append('/');
        builder.append(cpart);
    }
    m_parts = move(canonical_parts);
    m_string = builder.to_string();
    return true;
}

bool FileSystemPath::has_extension(StringView extension) const
{
    // FIXME: This is inefficient, expand StringView with enough functionality that we don't need to copy strings here.
    String extension_string = extension;
    return m_string.to_lowercase().ends_with(extension_string.to_lowercase());
}

}
