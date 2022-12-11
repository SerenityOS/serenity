/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Debug.h>
#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <AK/Vector.h>
#include <LibCore/Object.h>
#include <LibCore/Stream.h>

namespace SQL {

constexpr static u32 BLOCKSIZE = 1024;

/**
 * A Heap is a logical container for database (SQL) data. Conceptually a
 * Heap can be a database file, or a memory block, or another storage medium.
 * It contains datastructures, like B-Trees, hash_index tables, or tuple stores
 * (basically a list of data tuples).
 *
 * A Heap can be thought of the backing storage of a single database. It's
 * assumed that a single SQL database is backed by a single Heap.
 *
 * Currently only B-Trees and tuple stores are implemented.
 */
class Heap : public Core::Object {
    C_OBJECT(Heap);

public:
    static constexpr inline u32 current_version = 2;

    virtual ~Heap() override;

    ErrorOr<void> open();
    u32 size() const { return m_end_of_file; }
    ErrorOr<ByteBuffer> read_block(u32);
    [[nodiscard]] u32 new_record_pointer();
    [[nodiscard]] bool valid() const { return static_cast<bool>(m_file); }

    u32 schemas_root() const { return m_schemas_root; }

    void set_schemas_root(u32 root)
    {
        m_schemas_root = root;
        update_zero_block();
    }

    u32 tables_root() const { return m_tables_root; }

    void set_tables_root(u32 root)
    {
        m_tables_root = root;
        update_zero_block();
    }

    u32 table_columns_root() const { return m_table_columns_root; }

    void set_table_columns_root(u32 root)
    {
        m_table_columns_root = root;
        update_zero_block();
    }
    u32 version() const { return m_version; }

    u32 user_value(size_t index) const
    {
        VERIFY(index < m_user_values.size());
        return m_user_values[index];
    }

    void set_user_value(size_t index, u32 value)
    {
        VERIFY(index < m_user_values.size());
        m_user_values[index] = value;
        update_zero_block();
    }

    void add_to_wal(u32 block, ByteBuffer& buffer)
    {
        dbgln_if(SQL_DEBUG, "Adding to WAL: block #{}, size {}", block, buffer.size());
        dbgln_if(SQL_DEBUG, "{:hex-dump}", buffer.bytes().trim(8));
        m_write_ahead_log.set(block, buffer);
    }

    ErrorOr<void> flush();

private:
    explicit Heap(DeprecatedString);

    ErrorOr<void> write_block(u32, ByteBuffer&);
    ErrorOr<void> seek_block(u32);
    ErrorOr<void> read_zero_block();
    void initialize_zero_block();
    void update_zero_block();

    OwnPtr<Core::Stream::BufferedFile> m_file;
    u32 m_free_list { 0 };
    u32 m_next_block { 1 };
    u32 m_end_of_file { 1 };
    u32 m_schemas_root { 0 };
    u32 m_tables_root { 0 };
    u32 m_table_columns_root { 0 };
    u32 m_version { current_version };
    Array<u32, 16> m_user_values { 0 };
    HashMap<u32, ByteBuffer> m_write_ahead_log;
};

}
