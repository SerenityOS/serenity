/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/Format.h>
#include <AK/QuickSort.h>
#include <LibCore/IODevice.h>
#include <LibCore/System.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Serializer.h>
#include <sys/stat.h>
#include <sys/types.h>

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
    if (file_size > 0)
        m_next_block = m_end_of_file = file_size / BLOCKSIZE;

    auto file = TRY(Core::Stream::File::open(name(), Core::Stream::OpenMode::ReadWrite));
    m_file = TRY(Core::Stream::BufferedFile::create(move(file)));

    if (file_size > 0) {
        if (auto error_maybe = read_zero_block(); error_maybe.is_error()) {
            m_file = nullptr;
            return error_maybe.error();
        }
    } else {
        initialize_zero_block();
    }

    // FIXME: We should more gracefully handle version incompatibilities. For now, we drop the database.
    if (m_version != current_version) {
        dbgln_if(SQL_DEBUG, "Heap file {} opened has incompatible version {}. Deleting for version {}.", name(), m_version, current_version);
        m_file = nullptr;

        TRY(Core::System::unlink(name()));
        return open();
    }

    dbgln_if(SQL_DEBUG, "Heap file {} opened. Size = {}", name(), size());
    return {};
}

ErrorOr<ByteBuffer> Heap::read_block(u32 block)
{
    if (!m_file) {
        warnln("Heap({})::read_block({}): Heap file not opened"sv, name(), block);
        return Error::from_string_literal("Heap()::read_block(): Heap file not opened");
    }

    if (auto buffer = m_write_ahead_log.get(block); buffer.has_value())
        return TRY(ByteBuffer::copy(*buffer));

    if (block >= m_next_block) {
        warnln("Heap({})::read_block({}): block # out of range (>= {})"sv, name(), block, m_next_block);
        return Error::from_string_literal("Heap()::read_block(): block # out of range");
    }

    dbgln_if(SQL_DEBUG, "Read heap block {}", block);
    TRY(seek_block(block));

    auto buffer = TRY(ByteBuffer::create_uninitialized(BLOCKSIZE));
    auto bytes = TRY(m_file->read(buffer));

    dbgln_if(SQL_DEBUG, "{:hex-dump}", bytes.trim(8));
    TRY(buffer.try_resize(bytes.size()));

    return buffer;
}

ErrorOr<void> Heap::write_block(u32 block, ByteBuffer& buffer)
{
    if (!m_file) {
        warnln("Heap({})::write_block({}): Heap file not opened"sv, name(), block);
        return Error::from_string_literal("Heap()::write_block(): Heap file not opened");
    }
    if (block > m_next_block) {
        warnln("Heap({})::write_block({}): block # out of range (> {})"sv, name(), block, m_next_block);
        return Error::from_string_literal("Heap()::write_block(): block # out of range");
    }
    if (buffer.size() > BLOCKSIZE) {
        warnln("Heap({})::write_block({}): Oversized block ({} > {})"sv, name(), block, buffer.size(), BLOCKSIZE);
        return Error::from_string_literal("Heap()::write_block(): Oversized block");
    }

    dbgln_if(SQL_DEBUG, "Write heap block {} size {}", block, buffer.size());
    TRY(seek_block(block));

    if (auto current_size = buffer.size(); current_size < BLOCKSIZE) {
        TRY(buffer.try_resize(BLOCKSIZE));
        memset(buffer.offset_pointer(current_size), 0, BLOCKSIZE - current_size);
    }

    dbgln_if(SQL_DEBUG, "{:hex-dump}", buffer.bytes().trim(8));
    TRY(m_file->write(buffer));

    if (block == m_end_of_file)
        m_end_of_file++;
    return {};
}

ErrorOr<void> Heap::seek_block(u32 block)
{
    if (!m_file) {
        warnln("Heap({})::seek_block({}): Heap file not opened"sv, name(), block);
        return Error::from_string_literal("Heap()::seek_block(): Heap file not opened");
    }
    if (block > m_end_of_file) {
        warnln("Heap({})::seek_block({}): Cannot seek beyond end of file at block {}"sv, name(), block, m_end_of_file);
        return Error::from_string_literal("Heap()::seek_block(): Cannot seek beyond end of file");
    }

    if (block == m_end_of_file)
        TRY(m_file->seek(0, Core::Stream::SeekMode::FromEndPosition));
    else
        TRY(m_file->seek(block * BLOCKSIZE, Core::Stream::SeekMode::SetPosition));

    return {};
}

u32 Heap::new_record_pointer()
{
    VERIFY(m_file);
    if (m_free_list) {
        auto block_or_error = read_block(m_free_list);
        if (block_or_error.is_error()) {
            warnln("FREE LIST CORRUPTION");
            VERIFY_NOT_REACHED();
        }
        auto new_pointer = m_free_list;
        memcpy(&m_free_list, block_or_error.value().offset_pointer(0), sizeof(u32));
        update_zero_block();
        return new_pointer;
    }
    return m_next_block++;
}

ErrorOr<void> Heap::flush()
{
    VERIFY(m_file);
    Vector<u32> blocks;
    for (auto& wal_entry : m_write_ahead_log) {
        blocks.append(wal_entry.key);
    }
    quick_sort(blocks);
    for (auto& block : blocks) {
        auto buffer_it = m_write_ahead_log.find(block);
        VERIFY(buffer_it != m_write_ahead_log.end());
        dbgln_if(SQL_DEBUG, "Flushing block {} to {}", block, name());
        TRY(write_block(block, buffer_it->value));
    }
    m_write_ahead_log.clear();
    dbgln_if(SQL_DEBUG, "WAL flushed. Heap size = {}", size());
    return {};
}

constexpr static auto FILE_ID = "SerenitySQL "sv;
constexpr static auto VERSION_OFFSET = FILE_ID.length();
constexpr static auto SCHEMAS_ROOT_OFFSET = VERSION_OFFSET + sizeof(u32);
constexpr static auto TABLES_ROOT_OFFSET = SCHEMAS_ROOT_OFFSET + sizeof(u32);
constexpr static auto TABLE_COLUMNS_ROOT_OFFSET = TABLES_ROOT_OFFSET + sizeof(u32);
constexpr static auto FREE_LIST_OFFSET = TABLE_COLUMNS_ROOT_OFFSET + sizeof(u32);
constexpr static auto USER_VALUES_OFFSET = FREE_LIST_OFFSET + sizeof(u32);

ErrorOr<void> Heap::read_zero_block()
{
    auto buffer = TRY(read_block(0));
    auto file_id_buffer = TRY(buffer.slice(0, FILE_ID.length()));
    auto file_id = StringView(file_id_buffer);
    if (file_id != FILE_ID) {
        warnln("{}: Zero page corrupt. This is probably not a {} heap file"sv, name(), FILE_ID);
        return Error::from_string_literal("Heap()::read_zero_block(): Zero page corrupt. This is probably not a SerenitySQL heap file");
    }

    dbgln_if(SQL_DEBUG, "Read zero block from {}", name());

    memcpy(&m_version, buffer.offset_pointer(VERSION_OFFSET), sizeof(u32));
    dbgln_if(SQL_DEBUG, "Version: {}.{}", (m_version & 0xFFFF0000) >> 16, (m_version & 0x0000FFFF));

    memcpy(&m_schemas_root, buffer.offset_pointer(SCHEMAS_ROOT_OFFSET), sizeof(u32));
    dbgln_if(SQL_DEBUG, "Schemas root node: {}", m_schemas_root);

    memcpy(&m_tables_root, buffer.offset_pointer(TABLES_ROOT_OFFSET), sizeof(u32));
    dbgln_if(SQL_DEBUG, "Tables root node: {}", m_tables_root);

    memcpy(&m_table_columns_root, buffer.offset_pointer(TABLE_COLUMNS_ROOT_OFFSET), sizeof(u32));
    dbgln_if(SQL_DEBUG, "Table columns root node: {}", m_table_columns_root);

    memcpy(&m_free_list, buffer.offset_pointer(FREE_LIST_OFFSET), sizeof(u32));
    dbgln_if(SQL_DEBUG, "Free list: {}", m_free_list);

    memcpy(m_user_values.data(), buffer.offset_pointer(USER_VALUES_OFFSET), m_user_values.size() * sizeof(u32));
    for (auto ix = 0u; ix < m_user_values.size(); ix++) {
        if (m_user_values[ix]) {
            dbgln_if(SQL_DEBUG, "User value {}: {}", ix, m_user_values[ix]);
        }
    }
    return {};
}

void Heap::update_zero_block()
{
    dbgln_if(SQL_DEBUG, "Write zero block to {}", name());
    dbgln_if(SQL_DEBUG, "Version: {}.{}", (m_version & 0xFFFF0000) >> 16, (m_version & 0x0000FFFF));
    dbgln_if(SQL_DEBUG, "Schemas root node: {}", m_schemas_root);
    dbgln_if(SQL_DEBUG, "Tables root node: {}", m_tables_root);
    dbgln_if(SQL_DEBUG, "Table Columns root node: {}", m_table_columns_root);
    dbgln_if(SQL_DEBUG, "Free list: {}", m_free_list);
    for (auto ix = 0u; ix < m_user_values.size(); ix++) {
        if (m_user_values[ix]) {
            dbgln_if(SQL_DEBUG, "User value {}: {}", ix, m_user_values[ix]);
        }
    }

    // FIXME: Handle an OOM failure here.
    auto buffer = ByteBuffer::create_zeroed(BLOCKSIZE).release_value_but_fixme_should_propagate_errors();
    buffer.overwrite(0, FILE_ID.characters_without_null_termination(), FILE_ID.length());
    buffer.overwrite(VERSION_OFFSET, &m_version, sizeof(u32));
    buffer.overwrite(SCHEMAS_ROOT_OFFSET, &m_schemas_root, sizeof(u32));
    buffer.overwrite(TABLES_ROOT_OFFSET, &m_tables_root, sizeof(u32));
    buffer.overwrite(TABLE_COLUMNS_ROOT_OFFSET, &m_table_columns_root, sizeof(u32));
    buffer.overwrite(FREE_LIST_OFFSET, &m_free_list, sizeof(u32));
    buffer.overwrite(USER_VALUES_OFFSET, m_user_values.data(), m_user_values.size() * sizeof(u32));

    add_to_wal(0, buffer);
}

void Heap::initialize_zero_block()
{
    m_version = current_version;
    m_schemas_root = 0;
    m_tables_root = 0;
    m_table_columns_root = 0;
    m_next_block = 1;
    m_free_list = 0;
    for (auto& user : m_user_values) {
        user = 0u;
    }
    update_zero_block();
}

}
