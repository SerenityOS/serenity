/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <LibArchive/TarStream.h>
#include <string.h>

namespace Archive {
TarFileStream::TarFileStream(TarInputStream& tar_stream)
    : m_tar_stream(tar_stream)
    , m_generation(tar_stream.m_generation)
{
}

ErrorOr<Bytes> TarFileStream::read(Bytes bytes)
{
    // Verify that the stream has not advanced.
    VERIFY(m_tar_stream.m_generation == m_generation);

    auto header_size = TRY(m_tar_stream.header().size());

    auto to_read = min(bytes.size(), header_size - m_tar_stream.m_file_offset);

    auto slice = TRY(m_tar_stream.m_stream->read(bytes.trim(to_read)));
    m_tar_stream.m_file_offset += slice.size();

    return slice;
}

bool TarFileStream::is_eof() const
{
    // Verify that the stream has not advanced.
    VERIFY(m_tar_stream.m_generation == m_generation);

    auto header_size_or_error = m_tar_stream.header().size();
    if (header_size_or_error.is_error())
        return true;
    auto header_size = header_size_or_error.release_value();

    return m_tar_stream.m_stream->is_eof()
        || m_tar_stream.m_file_offset >= header_size;
}

ErrorOr<size_t> TarFileStream::write(ReadonlyBytes)
{
    // This is purely for wrapping and representing file contents in an archive.
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullOwnPtr<TarInputStream>> TarInputStream::construct(NonnullOwnPtr<Core::Stream::Stream> stream)
{
    auto tar_stream = TRY(adopt_nonnull_own_or_enomem(new (nothrow) TarInputStream(move(stream))));

    // Try and read the header.
    auto header_span = TRY(tar_stream->m_stream->read(Bytes(&tar_stream->m_header, sizeof(m_header))));
    if (header_span.size() != sizeof(m_header))
        return Error::from_string_literal("Failed to read the entire header");

    // Discard the rest of the block.
    TRY(tar_stream->m_stream->discard(block_size - sizeof(TarFileHeader)));

    return tar_stream;
}

TarInputStream::TarInputStream(NonnullOwnPtr<Core::Stream::Stream> stream)
    : m_stream(move(stream))
{
}

static constexpr unsigned long block_ceiling(unsigned long offset)
{
    return block_size * (1 + ((offset - 1) / block_size));
}

ErrorOr<void> TarInputStream::advance()
{
    if (finished())
        return Error::from_string_literal("Attempted to advance a finished stream");

    m_generation++;

    // Discard the pending bytes of the current entry.
    auto file_size = TRY(m_header.size());
    TRY(m_stream->discard(block_ceiling(file_size) - m_file_offset));
    m_file_offset = 0;

    // FIXME: This is not unlike the initial initialization. Maybe we should merge those two.
    auto header_span = TRY(m_stream->read(Bytes(&m_header, sizeof(m_header))));
    if (header_span.size() != sizeof(m_header))
        return Error::from_string_literal("Failed to read the entire header");

    if (!valid())
        return Error::from_string_literal("Header is not valid");

    // Discard the rest of the header block.
    TRY(m_stream->discard(block_size - sizeof(TarFileHeader)));

    return {};
}

bool TarInputStream::valid() const
{
    auto const header_magic = header().magic();
    auto const header_version = header().version();

    if (!((header_magic == gnu_magic && header_version == gnu_version)
            || (header_magic == ustar_magic && header_version == ustar_version)
            || (header_magic == posix1_tar_magic && header_version == posix1_tar_version)))
        return false;

    // POSIX.1-1988 tar does not have magic numbers, so we also need to verify the header checksum.
    if (header().checksum().is_error())
        return false;

    return header().checksum().release_value() == header().expected_checksum();
}

TarFileStream TarInputStream::file_contents()
{
    VERIFY(!finished());
    return TarFileStream(*this);
}

TarOutputStream::TarOutputStream(OutputStream& stream)
    : m_stream(stream)
{
}

void TarOutputStream::add_directory(String const& path, mode_t mode)
{
    VERIFY(!m_finished);
    TarFileHeader header {};
    header.set_size(0);
    header.set_filename_and_prefix(String::formatted("{}/", path)); // Old tar implementations assume directory names end with a /
    header.set_type_flag(TarFileType::Directory);
    header.set_mode(mode);
    header.set_magic(gnu_magic);
    header.set_version(gnu_version);
    header.calculate_checksum();
    VERIFY(m_stream.write_or_error(Bytes { &header, sizeof(header) }));
    u8 padding[block_size] = { 0 };
    VERIFY(m_stream.write_or_error(Bytes { &padding, block_size - sizeof(header) }));
}

void TarOutputStream::add_file(String const& path, mode_t mode, ReadonlyBytes bytes)
{
    VERIFY(!m_finished);
    TarFileHeader header {};
    header.set_size(bytes.size());
    header.set_filename_and_prefix(path);
    header.set_type_flag(TarFileType::NormalFile);
    header.set_mode(mode);
    header.set_magic(gnu_magic);
    header.set_version(gnu_version);
    header.calculate_checksum();
    VERIFY(m_stream.write_or_error(ReadonlyBytes { &header, sizeof(header) }));
    constexpr Array<u8, block_size> padding { 0 };
    VERIFY(m_stream.write_or_error(ReadonlyBytes { &padding, block_size - sizeof(header) }));
    size_t n_written = 0;
    while (n_written < bytes.size()) {
        n_written += m_stream.write(bytes.slice(n_written, min(bytes.size() - n_written, block_size)));
    }
    VERIFY(m_stream.write_or_error(ReadonlyBytes { &padding, block_size - (n_written % block_size) }));
}

void TarOutputStream::add_link(String const& path, mode_t mode, StringView link_name)
{
    VERIFY(!m_finished);
    TarFileHeader header {};
    header.set_size(0);
    header.set_filename_and_prefix(path);
    header.set_type_flag(TarFileType::SymLink);
    header.set_mode(mode);
    header.set_magic(gnu_magic);
    header.set_version(gnu_version);
    header.set_link_name(link_name);
    header.calculate_checksum();
    VERIFY(m_stream.write_or_error(Bytes { &header, sizeof(header) }));
    u8 padding[block_size] = { 0 };
    VERIFY(m_stream.write_or_error(Bytes { &padding, block_size - sizeof(header) }));
}

void TarOutputStream::finish()
{
    VERIFY(!m_finished);
    constexpr Array<u8, block_size> padding { 0 };
    m_stream.write_or_error(ReadonlyBytes { &padding, block_size }); // 2 empty records that are used to signify the end of the archive
    m_stream.write_or_error(ReadonlyBytes { &padding, block_size });
    m_finished = true;
}

}
