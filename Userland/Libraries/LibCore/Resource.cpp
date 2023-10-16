/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/Resource.h>
#include <LibCore/ResourceImplementation.h>

namespace Core {

Resource::Resource(String path, Scheme scheme, NonnullOwnPtr<Core::MappedFile> file)
    : m_path(move(path))
    , m_scheme(scheme)
    , m_data(move(file))
{
}

Resource::Resource(String path, Scheme scheme, ByteBuffer buffer)
    : m_path(move(path))
    , m_scheme(scheme)
    , m_data(move(buffer))
{
}

Resource::Resource(String path, Scheme scheme, DirectoryTag)
    : m_path(move(path))
    , m_scheme(scheme)
    , m_data(DirectoryTag {})
{
}

ErrorOr<NonnullRefPtr<Resource>> Resource::load_from_uri(StringView uri)
{
    return ResourceImplementation::the().load_from_uri(uri);
}

[[nodiscard]] String Resource::uri() const
{
    return MUST(String::formatted("{}://{}", m_scheme == Scheme::Resource ? "resource"sv : "file"sv, m_path));
}

[[nodiscard]] Optional<String> Resource::filesystem_path() const
{
    return ResourceImplementation::the().filesystem_path(*this);
}

[[nodiscard]] String Resource::filename() const
{
    return MUST(String::from_utf8(LexicalPath(m_path.bytes_as_string_view()).basename()));
}

[[nodiscard]] Vector<String> Resource::children() const
{
    return ResourceImplementation::the().child_names(*this);
}

[[nodiscard]] ByteBuffer Resource::clone_data() const
{
    return m_data.visit(
        [](NonnullOwnPtr<Core::MappedFile> const& file) { return MUST(ByteBuffer::copy(file->bytes())); },
        [](ByteBuffer const& buffer) { return buffer; },
        [](DirectoryTag) -> ByteBuffer { VERIFY_NOT_REACHED(); });
}

[[nodiscard]] ByteBuffer Resource::release_data() &&
{
    VERIFY(!m_data.has<DirectoryTag>());

    if (m_data.has<NonnullOwnPtr<Core::MappedFile>>())
        return MUST(ByteBuffer::copy(m_data.get<NonnullOwnPtr<Core::MappedFile>>()->bytes()));
    return move(m_data).get<ByteBuffer>();
}

[[nodiscard]] ReadonlyBytes Resource::data() const
{
    return m_data.visit(
        [](NonnullOwnPtr<Core::MappedFile> const& file) { return file->bytes(); },
        [](ByteBuffer const& buffer) { return buffer.bytes(); },
        [](DirectoryTag) -> ReadonlyBytes { VERIFY_NOT_REACHED(); });
}

[[nodiscard]] FixedMemoryStream Resource::stream() const
{
    return FixedMemoryStream(data());
}
}
