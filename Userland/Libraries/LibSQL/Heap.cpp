/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/Format.h>
#include <AK/QuickSort.h>
#include <LibCore/System.h>
#include <LibSQL/Heap.h>
#include <sys/stat.h>

namespace SQL {

Heap::Heap(DeprecatedString file_name)
{
    set_name(move(file_name));
}

Heap::~Heap()
{
    if (m_file && !m_write_ahead_log.is_empty()) {
        if (auto maybe_error = flush(); maybe_error.is_error())
            warnln("~Heap({}): {}", name(), maybe_error.error());
    }
}

ErrorOr<void> Heap::open()
{
    size_t file_size = 0;
    struct stat stat_buffer;
    if (stat(name().characters(), &stat_buffer) != 0) {
        if (errno != ENOENT) {
            warnln("Heap::open({}): could not stat: {}"sv, name(), strerror(errno));
            return Error::from_string_literal("Heap::open(): could not stat file");
        }
    } else if (!S_ISREG(stat_buffer.st_mode)) {
        warnln("Heap::open({}): can only use regular files"sv, name());
        return Error::from_string_literal("Heap::open(): can only use regular files");
    } else {
        file_size = stat_buffer.st_size;
    }
    if (file_size > 0) {
        m_next_block = file_size / Block::SIZE;
        m_highest_block_written = m_next_block - 1;
    }

    auto file = TRY(Core::File::open(name(), Core::File::OpenMode::ReadWrite));
    m_file = TRY(Core::BufferedFile::create(move(file)));

    if (file_size > 0) {
        if (auto error_maybe = read_zero_block(); error_maybe.is_error()) {
            m_file = nullptr;
            return error_maybe.release_error();
        }
    } else {
        TRY(initialize_zero_block());
    }

    // FIXME: We should more gracefully handle version incompatibilities. For now, we drop the database.
    if (m_version != VERSION) {
        dbgln_if(SQL_DEBUG, "Heap file {} opened has incompatible version {}. Deleting for version {}.", name(), m_version, VERSION);
        m_file = nullptr;

        TRY(Core::System::unlink(name()));
        return open();
    }

    dbgln_if(SQL_DEBUG, "Heap file {} opened; number of blocks = {}", name(), m_highest_block_written);
    return {};
}

bool Heap::has_block(Block::Index index) const
{
    return index <= m_highest_block_written || m_write_ahead_log.contains(index);
}

ErrorOr<ByteBuffer> Heap::read_storage(Block::Index index)
{
    dbgln_if(SQL_DEBUG, "{}({})", __FUNCTION__, index);

    // Reconstruct the data storage from a potential chain of blocks
    ByteBuffer data;
    while (index > 0) {
        auto block = TRY(read_block(index));
        dbgln_if(SQL_DEBUG, "  -> {} bytes", block.size_in_bytes());
        TRY(data.try_append(block.data().bytes().slice(0, block.size_in_bytes())));
        index = block.next_block();
    }
    return data;
}

ErrorOr<void> Heap::write_storage(Block::Index index, ReadonlyBytes data)
{
    dbgln_if(SQL_DEBUG, "{}({}, {} bytes)", __FUNCTION__, index, data.size());
    VERIFY(data.size() > 0);

    // Split up the storage across multiple blocks if necessary, creating a chain
    u32 remaining_size = static_cast<u32>(data.size());
    u32 offset_in_data = 0;
    while (remaining_size > 0) {
        auto block_data_size = AK::min(remaining_size, Block::DATA_SIZE);
        remaining_size -= block_data_size;
        auto next_block_index = (remaining_size > 0) ? request_new_block_index() : 0;

        auto block_data = TRY(ByteBuffer::create_uninitialized(block_data_size));
        block_data.bytes().overwrite(0, data.offset(offset_in_data), block_data_size);

        TRY(write_block({ index, block_data_size, next_block_index, move(block_data) }));

        index = next_block_index;
        offset_in_data += block_data_size;
    }
    return {};
}

ErrorOr<ByteBuffer> Heap::read_raw_block(Block::Index index)
{
    VERIFY(m_file);
    VERIFY(index < m_next_block);

    if (auto data = m_write_ahead_log.get(index); data.has_value())
        return data.value();

    TRY(m_file->seek(index * Block::SIZE, SeekMode::SetPosition));
    auto buffer = TRY(ByteBuffer::create_uninitialized(Block::SIZE));
    TRY(m_file->read_until_filled(buffer));
    return buffer;
}

ErrorOr<Block> Heap::read_block(Block::Index index)
{
    dbgln_if(SQL_DEBUG, "Read heap block {}", index);

    auto buffer = TRY(read_raw_block(index));
    auto size_in_bytes = *reinterpret_cast<u32*>(buffer.offset_pointer(0));
    auto next_block = *reinterpret_cast<Block::Index*>(buffer.offset_pointer(sizeof(u32)));
    auto data = TRY(buffer.slice(Block::HEADER_SIZE, Block::DATA_SIZE));

    return Block { index, size_in_bytes, next_block, move(data) };
}

ErrorOr<void> Heap::write_raw_block(Block::Index index, ReadonlyBytes data)
{
    dbgln_if(SQL_DEBUG, "Write raw block {}", index);

    VERIFY(m_file);
    VERIFY(data.size() == Block::SIZE);

    TRY(m_file->seek(index * Block::SIZE, SeekMode::SetPosition));
    TRY(m_file->write_until_depleted(data));

    if (index > m_highest_block_written)
        m_highest_block_written = index;

    return {};
}

ErrorOr<void> Heap::write_raw_block_to_wal(Block::Index index, ByteBuffer&& data)
{
    dbgln_if(SQL_DEBUG, "{}(): adding raw block {} to WAL", __FUNCTION__, index);
    VERIFY(index < m_next_block);
    VERIFY(data.size() == Block::SIZE);

    TRY(m_write_ahead_log.try_set(index, move(data)));

    return {};
}

ErrorOr<void> Heap::write_block(Block const& block)
{
    VERIFY(block.index() < m_next_block);
    VERIFY(block.next_block() < m_next_block);
    VERIFY(block.data().size() <= Block::DATA_SIZE);

    auto size_in_bytes = block.size_in_bytes();
    auto next_block = block.next_block();

    auto heap_data = TRY(ByteBuffer::create_zeroed(Block::SIZE));
    heap_data.overwrite(0, &size_in_bytes, sizeof(size_in_bytes));
    heap_data.overwrite(sizeof(size_in_bytes), &next_block, sizeof(next_block));

    block.data().bytes().copy_to(heap_data.bytes().slice(Block::HEADER_SIZE));

    return write_raw_block_to_wal(block.index(), move(heap_data));
}

ErrorOr<void> Heap::flush()
{
    VERIFY(m_file);
    auto indices = m_write_ahead_log.keys();
    quick_sort(indices);
    for (auto index : indices) {
        dbgln_if(SQL_DEBUG, "Flushing block {} to {}", index, name());
        auto& data = m_write_ahead_log.get(index).value();
        TRY(write_raw_block(index, data));
    }
    m_write_ahead_log.clear();
    dbgln_if(SQL_DEBUG, "WAL flushed; new number of blocks = {}", m_highest_block_written);
    return {};
}

constexpr static auto FILE_ID = "SerenitySQL "sv;
constexpr static auto VERSION_OFFSET = FILE_ID.length();
constexpr static auto SCHEMAS_ROOT_OFFSET = VERSION_OFFSET + sizeof(u32);
constexpr static auto TABLES_ROOT_OFFSET = SCHEMAS_ROOT_OFFSET + sizeof(u32);
constexpr static auto TABLE_COLUMNS_ROOT_OFFSET = TABLES_ROOT_OFFSET + sizeof(u32);
constexpr static auto USER_VALUES_OFFSET = TABLE_COLUMNS_ROOT_OFFSET + sizeof(u32);

ErrorOr<void> Heap::read_zero_block()
{
    dbgln_if(SQL_DEBUG, "Read zero block from {}", name());

    auto block = TRY(read_raw_block(0));
    auto file_id_buffer = TRY(block.slice(0, FILE_ID.length()));
    auto file_id = StringView(file_id_buffer);
    if (file_id != FILE_ID) {
        warnln("{}: Zero page corrupt. This is probably not a {} heap file"sv, name(), FILE_ID);
        return Error::from_string_literal("Heap()::read_zero_block(): Zero page corrupt. This is probably not a SerenitySQL heap file");
    }

    memcpy(&m_version, block.offset_pointer(VERSION_OFFSET), sizeof(u32));
    dbgln_if(SQL_DEBUG, "Version: {}.{}", (m_version & 0xFFFF0000) >> 16, (m_version & 0x0000FFFF));

    memcpy(&m_schemas_root, block.offset_pointer(SCHEMAS_ROOT_OFFSET), sizeof(u32));
    dbgln_if(SQL_DEBUG, "Schemas root node: {}", m_schemas_root);

    memcpy(&m_tables_root, block.offset_pointer(TABLES_ROOT_OFFSET), sizeof(u32));
    dbgln_if(SQL_DEBUG, "Tables root node: {}", m_tables_root);

    memcpy(&m_table_columns_root, block.offset_pointer(TABLE_COLUMNS_ROOT_OFFSET), sizeof(u32));
    dbgln_if(SQL_DEBUG, "Table columns root node: {}", m_table_columns_root);

    memcpy(m_user_values.data(), block.offset_pointer(USER_VALUES_OFFSET), m_user_values.size() * sizeof(u32));
    for (auto ix = 0u; ix < m_user_values.size(); ix++) {
        if (m_user_values[ix])
            dbgln_if(SQL_DEBUG, "User value {}: {}", ix, m_user_values[ix]);
    }
    return {};
}

ErrorOr<void> Heap::update_zero_block()
{
    dbgln_if(SQL_DEBUG, "Write zero block to {}", name());
    dbgln_if(SQL_DEBUG, "Version: {}.{}", (m_version & 0xFFFF0000) >> 16, (m_version & 0x0000FFFF));
    dbgln_if(SQL_DEBUG, "Schemas root node: {}", m_schemas_root);
    dbgln_if(SQL_DEBUG, "Tables root node: {}", m_tables_root);
    dbgln_if(SQL_DEBUG, "Table Columns root node: {}", m_table_columns_root);
    for (auto ix = 0u; ix < m_user_values.size(); ix++) {
        if (m_user_values[ix] > 0)
            dbgln_if(SQL_DEBUG, "User value {}: {}", ix, m_user_values[ix]);
    }

    auto buffer = TRY(ByteBuffer::create_zeroed(Block::SIZE));
    auto buffer_bytes = buffer.bytes();
    buffer_bytes.overwrite(0, FILE_ID.characters_without_null_termination(), FILE_ID.length());
    buffer_bytes.overwrite(VERSION_OFFSET, &m_version, sizeof(u32));
    buffer_bytes.overwrite(SCHEMAS_ROOT_OFFSET, &m_schemas_root, sizeof(u32));
    buffer_bytes.overwrite(TABLES_ROOT_OFFSET, &m_tables_root, sizeof(u32));
    buffer_bytes.overwrite(TABLE_COLUMNS_ROOT_OFFSET, &m_table_columns_root, sizeof(u32));
    buffer_bytes.overwrite(USER_VALUES_OFFSET, m_user_values.data(), m_user_values.size() * sizeof(u32));

    return write_raw_block_to_wal(0, move(buffer));
}

ErrorOr<void> Heap::initialize_zero_block()
{
    m_version = VERSION;
    m_schemas_root = 0;
    m_tables_root = 0;
    m_table_columns_root = 0;
    m_next_block = 1;
    for (auto& user : m_user_values)
        user = 0u;
    return update_zero_block();
}

}
