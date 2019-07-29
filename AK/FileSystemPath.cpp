#include "FileSystemPath.h"
#include "StringBuilder.h"
#include "Vector.h"
#include "kstdio.h"

namespace AK {

FileSystemPath::FileSystemPath(const StringView& s)
    : m_string(s)
{
    canonicalize();
    m_is_valid = true;
}

void FileSystemPath::canonicalize()
{
    auto parts = m_string.split_view('/');
    int approximate_canonical_length = 0;
    Vector<String> canonical_parts;

    for (auto& part : parts) {
        if (part == ".")
            continue;
        if (part == "..") {
            if (!canonical_parts.is_empty())
                canonical_parts.take_last();
            continue;
        }
        if (!part.is_empty()) {
            approximate_canonical_length += part.length() + 1;
            canonical_parts.append(part);
        }
    }
    if (canonical_parts.is_empty()) {
        m_string = m_basename = "/";
        return;
    }

    m_basename = canonical_parts.last();
    auto name_parts = m_basename.split('.');
    m_title = name_parts[0];
    if (name_parts.size() > 1)
        m_extension = name_parts[1];

    StringBuilder builder(approximate_canonical_length);
    for (auto& cpart : canonical_parts) {
        builder.append('/');
        builder.append(cpart);
    }
    m_parts = move(canonical_parts);
    m_string = builder.to_string();
}

bool FileSystemPath::has_extension(StringView extension) const
{
    // FIXME: This is inefficient, expand StringView with enough functionality that we don't need to copy strings here.
    String extension_string = extension;
    return m_string.to_lowercase().ends_with(extension_string.to_lowercase());
}

String canonicalized_path(const StringView& path)
{
    return FileSystemPath(path).string();
}

}
