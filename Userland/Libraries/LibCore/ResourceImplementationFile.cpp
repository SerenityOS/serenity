/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/StringView.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Resource.h>
#include <LibCore/ResourceImplementationFile.h>

namespace Core {

ResourceImplementationFile::ResourceImplementationFile(String base_directory)
    : m_base_directory(move(base_directory))
{
}

ErrorOr<NonnullRefPtr<Resource>> ResourceImplementationFile::load_from_resource_scheme_uri(StringView uri)
{
    StringView const resource_scheme = "resource://"sv;

    VERIFY(uri.starts_with(resource_scheme));

    auto path = TRY(String::from_utf8(uri.substring_view(resource_scheme.length())));
    auto full_path = TRY(String::from_deprecated_string(LexicalPath::join(m_base_directory, path).string()));
    if (is_directory(full_path))
        return make_directory_resource(move(path));

    return make_resource(path, TRY(MappedFile::map(full_path)));
}

Vector<String> ResourceImplementationFile::child_names_for_resource_scheme(Resource const& resource)
{
    Vector<String> children;
    Core::DirIterator it(resource.filesystem_path().to_deprecated_string(), Core::DirIterator::SkipParentAndBaseDir);
    while (it.has_next())
        children.append(MUST(String::from_deprecated_string(it.next_path())));

    return children;
}

String ResourceImplementationFile::filesystem_path_for_resource_scheme(String const& relative_path)
{
    return MUST(String::from_deprecated_string(LexicalPath::join(m_base_directory, relative_path).string()));
}

}
