/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Files.h"
#include <AK/ByteBuffer.h>
#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/RedBlackTree.h>
#include <AK/Vector.h>
#include <LibCore/File.h>

namespace BitTorrent {

class MultiFileMapperStream : public SeekableStream {
    AK_MAKE_NONCOPYABLE(MultiFileMapperStream);

public:
    static ErrorOr<NonnullOwnPtr<MultiFileMapperStream>> try_create(Vector<LocalFile> local_files);

    ErrorOr<void> read_until_filled(Bytes bytes) override;
    ErrorOr<size_t> seek(i64 offset, SeekMode mode) override;
    ErrorOr<void> truncate(size_t length) override;
    ErrorOr<size_t> tell() const override;
    ErrorOr<size_t> size() override;
    ErrorOr<void> discard(size_t discarded_bytes) override;
    ErrorOr<Bytes> read_some(Bytes bytes) override;

    ErrorOr<size_t> write_some(ReadonlyBytes bytes) override;
    ErrorOr<void> write_until_depleted(ReadonlyBytes bytes) override;
    bool is_eof() const override;
    bool is_open() const override;
    void close() override;
    ~MultiFileMapperStream();

    u64 total_size() const { return m_total_size; }

private:
    struct MappedFilePosition : public RefCounted<MappedFilePosition> {
        MappedFilePosition(size_t file_index, i64 relative_zero_offset, NonnullOwnPtr<SeekableStream> fs_file)
            : file_index(file_index)
            , relative_zero_offset(relative_zero_offset)
            , fs_file(move(fs_file))
        {
        }
        size_t const file_index;
        i64 const relative_zero_offset; // also used as the key of the BST
        NonnullOwnPtr<SeekableStream> const fs_file;
    };

    MultiFileMapperStream(Vector<NonnullRefPtr<MappedFilePosition>> files_positions, u64 total_size)
        : m_current_file(files_positions.first())
        , m_files_positions(move(files_positions))
        , m_total_size(total_size)
    {
        for (auto mapped_file_position : m_files_positions) {
            m_files_positions_by_offset.insert(mapped_file_position->relative_zero_offset, move(mapped_file_position));
        }
    }
    SeekableStream& current_fs_file()
    {
        return *m_current_file->fs_file;
    }

    NonnullRefPtr<MappedFilePosition> m_current_file;
    Vector<NonnullRefPtr<MappedFilePosition>> m_files_positions;
    RedBlackTree<u64, NonnullRefPtr<MappedFilePosition>> m_files_positions_by_offset;
    u64 const m_total_size;
    u64 m_current_offset { 0 };
};

class TorrentDataFileMap {
public:
    static ErrorOr<NonnullOwnPtr<TorrentDataFileMap>> try_create(i64 piece_length, Vector<LocalFile> files);

    ErrorOr<void> read_piece(u32 index, Bytes buffer);
    ErrorOr<bool> write_piece(u32 index, ReadonlyBytes data);
    ErrorOr<bool> validate_hash(i64 index, ReadonlyBytes data);

private:
    TorrentDataFileMap(i64 piece_length, NonnullOwnPtr<MultiFileMapperStream> mapper);
    u64 m_piece_length;
    NonnullOwnPtr<MultiFileMapperStream> m_files_mapper;
};

}
