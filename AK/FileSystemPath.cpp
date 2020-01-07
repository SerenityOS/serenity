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
    if (m_string.is_empty()) {
        m_parts.clear();
        return;
    }

    bool is_absolute_path = m_string[0] == '/';
    auto parts = m_string.split_view('/');

    if (!is_absolute_path)
        parts.prepend(".");

    int approximate_canonical_length = 0;
    Vector<String> canonical_parts;

    for (int i = 0; i < parts.size(); ++i) {
        auto& part = parts[i];
        if (is_absolute_path || i != 0) {
            if (part == ".")
                continue;
        }
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
        m_string = m_basename = m_dirname = "/";
        return;
    }

    StringBuilder dirname_builder(approximate_canonical_length);
    for (int i = 0; i < canonical_parts.size() - 1; ++i) {
        auto& canonical_part = canonical_parts[i];
        if (is_absolute_path || i != 0)
            dirname_builder.append('/');
        dirname_builder.append(canonical_part);
    }
    m_dirname = dirname_builder.to_string();

    m_basename = canonical_parts.last();
    auto name_parts = m_basename.split('.');
    m_title = name_parts[0];
    if (name_parts.size() > 1)
        m_extension = name_parts[1];

    StringBuilder builder(approximate_canonical_length);
    for (int i = 0; i < canonical_parts.size(); ++i) {
        auto& canonical_part = canonical_parts[i];
        if (is_absolute_path || i != 0)
            builder.append('/');
        builder.append(canonical_part);
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
