/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <LibCore/IODevice.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Serializer.h>
#include <sys/stat.h>
#include <sys/types.h>

namespace SQL {

Heap::Heap(String file_name)
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
            return Error::from_string_literal("Heap::open(): could not stat file"sv);
        }
    } else if (!S_ISREG(stat_buffer.st_mode)) {
        warnln("Heap::open({}): can only use regular files"sv, name());
        return Error::from_string_literal("Heap::open(): can only use regular files"sv);
    } else {
        file_size = stat_buffer.st_size;
    }
    if (file_size > 0)
        m_next_block = m_end_of_file = file_size / BLOCKSIZE;

    auto file_or_error = Core::File::open(name(), Core::OpenMode::ReadWrite);
    if (file_or_error.is_error()) {
        warnln("Heap::open({}): could not open: {}"sv, name(), file_or_error.error());
        return Error::from_string_literal("Heap::open(): could not open file"sv);
    }
    m_file = file_or_error.value();
    if (file_size > 0) {
        if (auto error_maybe = read_zero_block(); error_maybe.is_error()) {
            m_file = nullptr;
            return error_maybe.error();
        }
    } else {
        initialize_zero_block();
    }
    dbgln_if(SQL_DEBUG, "Heap file {} opened. Size = {}", name(), size());
    return {};
}

ErrorOr<ByteBuffer> Heap::read_block(u32 block)
{
    if (m_file.is_null()) {
        warnln("Heap({})::read_block({}): Heap file not opened"sv, name(), block);
        return Error::from_string_literal("Heap()::read_block(): Heap file not opened"sv);
    }
    auto buffer_or_empty = m_write_ahead_log.get(block);
    if (buffer_or_empty.has_value())
        return buffer_or_empty.release_value();

    if (block >= m_next_block) {
        warnln("Heap({})::read_block({}): block # out of range (>= {})"sv, name(), block, m_next_block);
        return Error::from_string_literal("Heap()::read_block(): block # out of range"sv);
    }
    dbgln_if(SQL_DEBUG, "Read heap block {}", block);
    TRY(seek_block(block));
    auto ret = m_file->read(BLOCKSIZE);
    if (ret.is_empty()) {
        warnln("Heap({})::read_block({}): Could not read block"sv, name(), block);
        return Error::from_string_literal("Heap()::read_block(): Could not read block"sv);
    }
    dbgln_if(SQL_DEBUG, "{:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x}",
        *ret.offset_pointer(0), *ret.offset_pointer(1),
        *ret.offset_pointer(2), *ret.offset_pointer(3),
        *ret.offset_pointer(4), *ret.offset_pointer(5),
        *ret.offset_pointer(6), *ret.offset_pointer(7));
    return ret;
}

ErrorOr<void> Heap::write_block(u32 block, ByteBuffer& buffer)
{
    if (m_file.is_null()) {
        warnln("Heap({})::write_block({}): Heap file not opened"sv, name(), block);
        return Error::from_string_literal("Heap()::write_block(): Heap file not opened"sv);
    }
    if (block > m_next_block) {
        warnln("Heap({})::write_block({}): block # out of range (> {})"sv, name(), block, m_next_block);
        return Error::from_string_literal("Heap()::write_block(): block # out of range"sv);
    }
    TRY(seek_block(block));
    dbgln_if(SQL_DEBUG, "Write heap block {} size {}", block, buffer.size());
    if (buffer.size() > BLOCKSIZE) {
        warnln("Heap({})::write_block({}): Oversized block ({} > {})"sv, name(), block, buffer.size(), BLOCKSIZE);
        return Error::from_string_literal("Heap()::write_block(): Oversized block"sv);
    }
    auto sz = buffer.size();
    if (sz < BLOCKSIZE) {
        if (buffer.try_resize(BLOCKSIZE).is_error()) {
            warnln("Heap({})::write_block({}): Could not align block of size {} to {}"sv, name(), block, buffer.size(), BLOCKSIZE);
            return Error::from_string_literal("Heap()::write_block(): Could not align block"sv);
        }
        memset(buffer.offset_pointer((int)sz), 0, BLOCKSIZE - sz);
    }
    dbgln_if(SQL_DEBUG, "{:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x} {:02x}",
        *buffer.offset_pointer(0), *buffer.offset_pointer(1),
        *buffer.offset_pointer(2), *buffer.offset_pointer(3),
        *buffer.offset_pointer(4), *buffer.offset_pointer(5),
        *buffer.offset_pointer(6), *buffer.offset_pointer(7));
    if (m_file->write(buffer.data(), (int)buffer.size())) {
        if (block == m_end_of_file)
            m_end_of_file++;
        return {};
    }
    warnln("Heap({})::write_block({}): Could not full write block"sv, name(), block);
    return Error::from_string_literal("Heap()::write_block(): Could not full write block"sv);
}

ErrorOr<void> Heap::seek_block(u32 block)
{
    if (m_file.is_null()) {
        warnln("Heap({})::seek_block({}): Heap file not opened"sv, name(), block);
        return Error::from_string_literal("Heap()::seek_block(): Heap file not opened"sv);
    }
    if (block == m_end_of_file) {
        off_t pos;
        if (!m_file->seek(0, Core::SeekMode::FromEndPosition, &pos)) {
            warnln("Heap({})::seek_block({}): Error seeking end of file: {}"sv, name(), block, m_file->error_string());
            return Error::from_string_literal("Heap()::seek_block(): Error seeking end of file"sv);
        }
    } else if (block > m_end_of_file) {
        warnln("Heap({})::seek_block({}): Cannot seek beyond end of file at block {}"sv, name(), block, m_end_of_file);
        return Error::from_string_literal("Heap()::seek_block(): Cannot seek beyond end of file"sv);
    } else {
        if (!m_file->seek(block * BLOCKSIZE)) {
            warnln("Heap({})::seek_block({}): Error seeking: {}"sv, name(), block, m_file->error_string());
            return Error::from_string_literal("Heap()::seek_block(): Error seeking: {}"sv);
        }
    }
    return {};
}

u32 Heap::new_record_pointer()
{
    VERIFY(!m_file.is_null());
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
    VERIFY(!m_file.is_null());
    Vector<u32> blocks;
    for (auto& wal_entry : m_write_ahead_log) {
        blocks.append(wal_entry.key);
    }
    quick_sort(blocks);
    for (auto& block : blocks) {
        auto buffer_or_empty = m_write_ahead_log.get(block);
        if (buffer_or_empty->is_empty()) {
            VERIFY_NOT_REACHED();
        }
        dbgln_if(SQL_DEBUG, "Flushing block {} to {}", block, name());
        TRY(write_block(block, buffer_or_empty.value()));
    }
    m_write_ahead_log.clear();
    dbgln_if(SQL_DEBUG, "WAL flushed. Heap size = {}", size());
    return {};
}

constexpr static StringView FILE_ID = "SerenitySQL "sv;
constexpr static int VERSION_OFFSET = 12;
constexpr static int SCHEMAS_ROOT_OFFSET = 16;
constexpr static int TABLES_ROOT_OFFSET = 20;
constexpr static int TABLE_COLUMNS_ROOT_OFFSET = 24;
constexpr static int FREE_LIST_OFFSET = 28;
constexpr static int USER_VALUES_OFFSET = 32;

ErrorOr<void> Heap::read_zero_block()
{
    auto buffer = TRY(read_block(0));
    auto file_id_buffer = buffer.slice(0, FILE_ID.length());
    auto file_id = StringView(file_id_buffer);
    if (file_id != FILE_ID) {
        warnln("{}: Zero page corrupt. This is probably not a {} heap file"sv, name(), FILE_ID);
        return Error::from_string_literal("Heap()::read_zero_block(): Zero page corrupt. This is probably not a SerenitySQL heap file"sv);
    }
    dbgln_if(SQL_DEBUG, "Read zero block from {}", name());
    memcpy(&m_version, buffer.offset_pointer(VERSION_OFFSET), sizeof(u32));
    dbgln_if(SQL_DEBUG, "Version: {}.{}", (m_version & 0xFFFF0000) >> 16, (m_version & 0x0000FFFF));
    memcpy(&m_schemas_root, buffer.offset_pointer(SCHEMAS_ROOT_OFFSET), sizeof(u32));
    dbgln_if(SQL_DEBUG, "Schemas root node: {}", m_tables_root);
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
    auto buffer = ByteBuffer::create_zeroed(BLOCKSIZE).release_value();
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
    m_version = 0x00000001;
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
