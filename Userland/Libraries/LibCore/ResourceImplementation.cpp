/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/DirIterator.h>
#include <LibCore/ResourceImplementation.h>
#include <LibCore/ResourceImplementationFile.h>
#include <LibCore/System.h>

namespace Core {

static OwnPtr<ResourceImplementation> s_the;

void ResourceImplementation::install(OwnPtr<ResourceImplementation> the)
{
    s_the = move(the);
}

ResourceImplementation& ResourceImplementation::the()
{
    if (!s_the)
        install(make<ResourceImplementationFile>("/res"_string));
    return *s_the;
}

NonnullRefPtr<Resource> ResourceImplementation::make_resource(String full_path, NonnullOwnPtr<Core::MappedFile> file)
{
    return adopt_ref(*new Resource(move(full_path), Resource::Scheme::Resource, move(file)));
}

NonnullRefPtr<Resource> ResourceImplementation::make_resource(String full_path, ByteBuffer buffer)
{
    return adopt_ref(*new Resource(move(full_path), Resource::Scheme::Resource, move(buffer)));
}

NonnullRefPtr<Resource> ResourceImplementation::make_directory_resource(String full_path)
{
    return adopt_ref(*new Resource(move(full_path), Resource::Scheme::Resource, Resource::DirectoryTag {}));
}

ErrorOr<NonnullRefPtr<Resource>> ResourceImplementation::load_from_uri(StringView uri)
{
    StringView const file_scheme = "file://"sv;
    StringView const resource_scheme = "resource://"sv;

    if (uri.starts_with(resource_scheme))
        return load_from_resource_scheme_uri(uri);

    if (uri.starts_with(file_scheme)) {
        auto path = uri.substring_view(file_scheme.length());
        if (is_directory(path))
            return adopt_ref(*new Resource(TRY(String::from_utf8(path)), Resource::Scheme::File, Resource::DirectoryTag {}));
        return adopt_ref(*new Resource(TRY(String::from_utf8(path)), Resource::Scheme::File, TRY(MappedFile::map(path))));
    }

    dbgln("ResourceImplementation: Unknown scheme for {}", uri);
    return Error::from_string_view("Invalid scheme"sv);
}

Vector<String> ResourceImplementation::child_names(Resource const& resource)
{
    if (!resource.is_directory())
        return {};

    if (resource.m_scheme == Resource::Scheme::Resource)
        return child_names_for_resource_scheme(resource);

    VERIFY(resource.m_scheme == Resource::Scheme::File);

    Vector<String> children;
    Core::DirIterator it(resource.filesystem_path().to_deprecated_string(), Core::DirIterator::SkipParentAndBaseDir);
    while (it.has_next())
        children.append(MUST(String::from_deprecated_string(it.next_path())));

    return children;
}

String ResourceImplementation::filesystem_path(Resource const& resource)
{
    if (resource.m_scheme == Resource::Scheme::Resource)
        return filesystem_path_for_resource_scheme(resource.m_path);

    VERIFY(resource.m_scheme == Resource::Scheme::File);

    return resource.m_path;
}

// Note: This is a copy of the impl in LibFilesystem, but we can't link that to LibCore
bool ResourceImplementation::is_directory(StringView filesystem_path)
{
    auto st_or_error = System::stat(filesystem_path);
    if (st_or_error.is_error())
        return false;
    auto st = st_or_error.release_value();
    return S_ISDIR(st.st_mode);
}

}
