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

size_t TarFileStream::read(Bytes bytes)
{
    // verify that the stream has not advanced
    VERIFY(m_tar_stream.m_generation == m_generation);

    if (has_any_error())
        return 0;

    auto to_read = min(bytes.size(), m_tar_stream.header().size() - m_tar_stream.m_file_offset);

    auto nread = m_tar_stream.m_stream.read(bytes.trim(to_read));
    m_tar_stream.m_file_offset += nread;
    return nread;
}

bool TarFileStream::unreliable_eof() const
{
    // verify that the stream has not advanced
    VERIFY(m_tar_stream.m_generation == m_generation);

    return m_tar_stream.m_stream.unreliable_eof()
        || m_tar_stream.m_file_offset >= m_tar_stream.header().size();
}

bool TarFileStream::read_or_error(Bytes bytes)
{
    // verify that the stream has not advanced
    VERIFY(m_tar_stream.m_generation == m_generation);

    if (read(bytes) < bytes.size()) {
        set_fatal_error();
        return false;
    }

    return true;
}

bool TarFileStream::discard_or_error(size_t count)
{
    // verify that the stream has not advanced
    VERIFY(m_tar_stream.m_generation == m_generation);

    if (count > m_tar_stream.header().size() - m_tar_stream.m_file_offset) {
        return false;
    }
    m_tar_stream.m_file_offset += count;
    return m_tar_stream.m_stream.discard_or_error(count);
}

TarInputStream::TarInputStream(InputStream& stream)
    : m_stream(stream)
{
    if (!m_stream.read_or_error(Bytes(&m_header, sizeof(m_header)))) {
        m_finished = true;
        m_stream.handle_any_error(); // clear out errors so we dont assert
        return;
    }
    VERIFY(m_stream.discard_or_error(block_size - sizeof(TarFileHeader)));
}

static constexpr unsigned long block_ceiling(unsigned long offset)
{
    return block_size * (1 + ((offset - 1) / block_size));
}

void TarInputStream::advance()
{
    if (m_finished)
        return;

    m_generation++;
    VERIFY(m_stream.discard_or_error(block_ceiling(m_header.size()) - m_file_offset));
    m_file_offset = 0;

    if (!m_stream.read_or_error(Bytes(&m_header, sizeof(m_header)))) {
        m_finished = true;
        return;
    }
    if (!valid()) {
        m_finished = true;
        return;
    }

    VERIFY(m_stream.discard_or_error(block_size - sizeof(TarFileHeader)));
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
    return header().checksum() == header().expected_checksum();
}

TarFileStream TarInputStream::file_contents()
{
    VERIFY(!m_finished);
    return TarFileStream(*this);
}

TarOutputStream::TarOutputStream(OutputStream& stream)
    : m_stream(stream)
{
}

void TarOutputStream::add_directory(const String& path, mode_t mode)
{
    VERIFY(!m_finished);
    TarFileHeader header {};
    header.set_size(0);
    header.set_filename(String::formatted("{}/", path)); // Old tar implementations assume directory names end with a /
    header.set_type_flag(TarFileType::Directory);
    header.set_mode(mode);
    header.set_magic(gnu_magic);
    header.set_version(gnu_version);
    header.calculate_checksum();
    VERIFY(m_stream.write_or_error(Bytes { &header, sizeof(header) }));
    u8 padding[block_size] = { 0 };
    VERIFY(m_stream.write_or_error(Bytes { &padding, block_size - sizeof(header) }));
}

void TarOutputStream::add_file(const String& path, mode_t mode, ReadonlyBytes bytes)
{
    VERIFY(!m_finished);
    TarFileHeader header {};
    header.set_size(bytes.size());
    header.set_filename(path);
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

void TarOutputStream::finish()
{
    VERIFY(!m_finished);
    constexpr Array<u8, block_size> padding { 0 };
    m_stream.write_or_error(ReadonlyBytes { &padding, block_size }); // 2 empty records that are used to signify the end of the archive
    m_stream.write_or_error(ReadonlyBytes { &padding, block_size });
    m_finished = true;
}

}
