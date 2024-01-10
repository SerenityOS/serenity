/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/ByteString.h>
#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibCore/File.h>

namespace SQL {

/**
 * A Block represents a single discrete chunk of 1024 bytes inside the Heap, and
 * acts as the container format for the actual data we are storing. This structure
 * is used for everything except block 0, the zero / super block.
 *
 * If data needs to be stored that is larger than 1016 bytes, Blocks are chained
 * together by setting the next block index and the data is reconstructed by
 * repeatedly reading blocks until the next block index is 0.
 */
class Block {
public:
    typedef u32 Index;

    static constexpr u32 SIZE = 1024;
    static constexpr u32 HEADER_SIZE = sizeof(u32) + sizeof(Index);
    static constexpr u32 DATA_SIZE = SIZE - HEADER_SIZE;

    Block(Index index, u32 size_in_bytes, Index next_block, ByteBuffer data)
        : m_index(index)
        , m_size_in_bytes(size_in_bytes)
        , m_next_block(next_block)
        , m_data(move(data))
    {
        VERIFY(index > 0);
    }

    Index index() const { return m_index; }
    u32 size_in_bytes() const { return m_size_in_bytes; }
    Index next_block() const { return m_next_block; }
    ByteBuffer const& data() const { return m_data; }

private:
    Index m_index;
    u32 m_size_in_bytes;
    Index m_next_block;
    ByteBuffer m_data;
};

/**
 * A Heap is a logical container for database (SQL) data. Conceptually a
 * Heap can be a database file, or a memory block, or another storage medium.
 * It contains datastructures, like B-Trees, hash_index tables, or tuple stores
 * (basically a list of data tuples).
 *
 * A Heap can be thought of the backing storage of a single database. It's
 * assumed that a single SQL database is backed by a single Heap.
 */
class Heap : public RefCounted<Heap> {
public:
    static constexpr u32 VERSION = 5;

    static ErrorOr<NonnullRefPtr<Heap>> create(ByteString);
    virtual ~Heap();

    ByteString const& name() const { return m_name; }

    ErrorOr<void> open();
    ErrorOr<size_t> file_size_in_bytes() const;

    [[nodiscard]] bool has_block(Block::Index) const;
    [[nodiscard]] Block::Index request_new_block_index();

    Block::Index schemas_root() const { return m_schemas_root; }

    void set_schemas_root(Block::Index root)
    {
        m_schemas_root = root;
        update_zero_block().release_value_but_fixme_should_propagate_errors();
    }

    Block::Index tables_root() const { return m_tables_root; }

    void set_tables_root(Block::Index root)
    {
        m_tables_root = root;
        update_zero_block().release_value_but_fixme_should_propagate_errors();
    }

    Block::Index table_columns_root() const { return m_table_columns_root; }

    void set_table_columns_root(Block::Index root)
    {
        m_table_columns_root = root;
        update_zero_block().release_value_but_fixme_should_propagate_errors();
    }
    u32 version() const { return m_version; }

    u32 user_value(size_t index) const
    {
        return m_user_values[index];
    }

    void set_user_value(size_t index, u32 value)
    {
        m_user_values[index] = value;
        update_zero_block().release_value_but_fixme_should_propagate_errors();
    }

    ErrorOr<ByteBuffer> read_storage(Block::Index);
    ErrorOr<void> write_storage(Block::Index, ReadonlyBytes);
    ErrorOr<void> free_storage(Block::Index);

    ErrorOr<void> flush();

private:
    explicit Heap(ByteString);

    ErrorOr<ByteBuffer> read_raw_block(Block::Index);
    ErrorOr<void> write_raw_block(Block::Index, ReadonlyBytes);
    ErrorOr<void> write_raw_block_to_wal(Block::Index, ByteBuffer&&);

    ErrorOr<Block> read_block(Block::Index);
    ErrorOr<void> write_block(Block const&);
    ErrorOr<void> free_block(Block const&);

    ErrorOr<void> read_zero_block();
    ErrorOr<void> initialize_zero_block();
    ErrorOr<void> update_zero_block();

    ByteString m_name;

    OwnPtr<Core::InputBufferedFile> m_file;
    Block::Index m_highest_block_written { 0 };
    Block::Index m_next_block { 1 };
    Block::Index m_schemas_root { 0 };
    Block::Index m_tables_root { 0 };
    Block::Index m_table_columns_root { 0 };
    u32 m_version { VERSION };
    Array<u32, 16> m_user_values { 0 };
    HashMap<Block::Index, ByteBuffer> m_write_ahead_log;
    Vector<Block::Index> m_free_block_indices;
};

}
