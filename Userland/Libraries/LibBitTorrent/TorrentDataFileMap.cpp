/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TorrentDataFileMap.h"
#include <LibCrypto/Hash/HashManager.h>

namespace BitTorrent {

ErrorOr<NonnullOwnPtr<MultiFileMapperStream>> MultiFileMapperStream::try_create(Vector<LocalFile> local_files)
{
    size_t i = 0;
    u64 total_size = 0;
    auto files_positions = Vector<NonnullRefPtr<MappedFilePosition>>();
    for (auto file : local_files) {
        total_size += file.size;

        auto fs_file = TRY(Core::InputBufferedFile::create(TRY(Core::File::open(file.path, Core::File::OpenMode::ReadWrite | Core::File::OpenMode::DontCreate))));
        auto mapped_file_position = make_ref_counted<MappedFilePosition>(i, total_size, move(fs_file));
        files_positions.append(move(mapped_file_position));
    }
    return adopt_nonnull_own_or_enomem(new (nothrow) MultiFileMapperStream(move(files_positions), total_size));
}

MultiFileMapperStream::~MultiFileMapperStream()
{
    close();
}

ErrorOr<void> MultiFileMapperStream::read_until_filled(Bytes buffer)
{
    size_t nread = 0;

    while (nread < buffer.size()) {
        if (current_fs_file().is_eof()) {
            if (m_current_file->file_index == m_files_positions.size() - 1)
                return Error::from_string_view_or_print_error_and_return_errno("Reached end-of-file before filling the entire buffer"sv, EIO);

            m_current_file = m_files_positions.at(m_current_file->file_index + 1);
            TRY(current_fs_file().seek(0, SeekMode::SetPosition));
        }

        auto result = read_some(buffer.slice(nread));
        if (result.is_error()) {
            if (result.error().is_errno() && result.error().code() == EINTR) {
                continue;
            }

            return result.release_error();
        }

        m_current_offset += result.value().size();
        nread += result.value().size();
    }

    return {};
}

ErrorOr<size_t> MultiFileMapperStream::seek(i64 offset, SeekMode mode)
{
    VERIFY(mode == SeekMode::SetPosition);

    if ((u64)offset == m_current_offset)
        return offset;

    auto* mapped_file_position = m_files_positions_by_offset.find_smallest_not_below(offset);
    if (!mapped_file_position)
        return Error::from_string_literal("Invalid offset");

    m_current_file = *mapped_file_position;
    u64 prev_relative_zero_offset = m_current_file->file_index == 0 ? 0 : m_files_positions.at(m_current_file->file_index - 1)->relative_zero_offset;

    TRY(m_current_file->fs_file->seek(offset - prev_relative_zero_offset, SeekMode::SetPosition));
    m_current_offset = (u64)offset;

    return offset;
}

ErrorOr<void> MultiFileMapperStream::truncate(size_t)
{
    VERIFY_NOT_REACHED();
}
ErrorOr<size_t> MultiFileMapperStream::tell() const
{
    VERIFY_NOT_REACHED();
}
ErrorOr<size_t> MultiFileMapperStream::size()
{
    return m_total_size;
}
ErrorOr<void> MultiFileMapperStream::discard(size_t)
{
    VERIFY_NOT_REACHED();
}
ErrorOr<Bytes> MultiFileMapperStream::read_some(Bytes bytes)
{
    return current_fs_file().read_some(bytes);
}

ErrorOr<size_t> MultiFileMapperStream::write_some(ReadonlyBytes bytes)
{
    return current_fs_file().write_some(bytes);
}

// TODO test writing a piece that spans multiple files
ErrorOr<void> MultiFileMapperStream::write_until_depleted(ReadonlyBytes buffer)
{
    size_t nwritten = 0;
    while (nwritten < buffer.size()) {
        if (current_fs_file().is_eof()) {
            dbgln("Writing to file but the current one is eof {}, moving to {}", m_current_file->file_index, m_current_file->file_index + 1);
            if (m_current_file->file_index == m_files_positions.size() - 1)
                return Error::from_string_view_or_print_error_and_return_errno("Reached end-of-file before filling the entire buffer"sv, EIO);

            m_current_file = m_files_positions.at(m_current_file->file_index + 1);
            TRY(current_fs_file().seek(0, SeekMode::SetPosition));
        }

        auto result = write_some(buffer.slice(nwritten));
        if (result.is_error()) {
            if (result.error().is_errno() && result.error().code() == EINTR) {
                continue;
            }

            return result.release_error();
        }

        m_current_offset += result.value();
        nwritten += result.value();
    }

    return {};
}
bool MultiFileMapperStream::is_eof() const
{
    VERIFY_NOT_REACHED();
}
bool MultiFileMapperStream::is_open() const
{
    VERIFY_NOT_REACHED();
}
void MultiFileMapperStream::close()
{
    for (auto fp : m_files_positions) {
        fp->fs_file->close();
    }
}

ErrorOr<NonnullOwnPtr<TorrentDataFileMap>> TorrentDataFileMap::try_create(i64 piece_length, Vector<LocalFile> local_files)
{
    auto mapper = TRY(MultiFileMapperStream::try_create(move(local_files)));
    return adopt_nonnull_own_or_enomem(new (nothrow) TorrentDataFileMap(piece_length, move(mapper)));
}

TorrentDataFileMap::TorrentDataFileMap(i64 piece_length, NonnullOwnPtr<MultiFileMapperStream> mapper)
    : m_piece_length(piece_length)
    , m_files_mapper(move(mapper))
{
}

ErrorOr<void> TorrentDataFileMap::read_piece(u32 index, Bytes buffer)
{
    if (buffer.size() > m_piece_length)
        return Error::from_string_view_or_print_error_and_return_errno("Invalid buffer size"sv, EINVAL);
    TRY(m_files_mapper->seek(index * m_piece_length, SeekMode::SetPosition));
    TRY(m_files_mapper->read_until_filled(buffer));
    return {};
}

ErrorOr<bool> TorrentDataFileMap::write_piece(u32 index, ReadonlyBytes data)
{
    TRY(m_files_mapper->seek(index * m_piece_length, SeekMode::SetPosition));
    TRY(m_files_mapper->write_until_depleted(data));
    return true;
}

}
