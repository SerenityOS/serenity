/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/IntrusiveList.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/BlockBasedFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

struct CacheEntry {
    IntrusiveListNode list_node;
    BlockBasedFS::BlockIndex block_index { 0 };
    u8* data { nullptr };
    bool has_data { false };
};

class DiskCache {
public:
    explicit DiskCache(BlockBasedFS& fs)
        : m_fs(fs)
        , m_cached_block_data(KBuffer::create_with_size(m_entry_count * m_fs.block_size()))
        , m_entries(KBuffer::create_with_size(m_entry_count * sizeof(CacheEntry)))
    {
        for (size_t i = 0; i < m_entry_count; ++i) {
            entries()[i].data = m_cached_block_data.data() + i * m_fs.block_size();
            m_clean_list.append(entries()[i]);
        }
    }

    ~DiskCache() = default;

    bool is_dirty() const { return m_dirty; }
    void set_dirty(bool b) { m_dirty = b; }

    void mark_all_clean()
    {
        while (auto* entry = m_dirty_list.first())
            m_clean_list.prepend(*entry);
        m_dirty = false;
    }

    void mark_dirty(CacheEntry& entry)
    {
        m_dirty_list.prepend(entry);
        m_dirty = true;
    }

    void mark_clean(CacheEntry& entry)
    {
        m_clean_list.prepend(entry);
    }

    CacheEntry& get(BlockBasedFS::BlockIndex block_index) const
    {
        if (auto it = m_hash.find(block_index); it != m_hash.end()) {
            auto& entry = const_cast<CacheEntry&>(*it->value);
            VERIFY(entry.block_index == block_index);
            return entry;
        }

        if (m_clean_list.is_empty()) {
            // Not a single clean entry! Flush writes and try again.
            // NOTE: We want to make sure we only call FileBackedFS flush here,
            //       not some FileBackedFS subclass flush!
            m_fs.flush_writes_impl();
            return get(block_index);
        }

        VERIFY(m_clean_list.last());
        auto& new_entry = *m_clean_list.last();
        m_clean_list.prepend(new_entry);

        m_hash.remove(new_entry.block_index);
        m_hash.set(block_index, &new_entry);

        new_entry.block_index = block_index;
        new_entry.has_data = false;

        return new_entry;
    }

    const CacheEntry* entries() const { return (const CacheEntry*)m_entries.data(); }
    CacheEntry* entries() { return (CacheEntry*)m_entries.data(); }

    template<typename Callback>
    void for_each_dirty_entry(Callback callback)
    {
        for (auto& entry : m_dirty_list)
            callback(entry);
    }

private:
    BlockBasedFS& m_fs;
    size_t m_entry_count { 10000 };
    mutable HashMap<BlockBasedFS::BlockIndex, CacheEntry*> m_hash;
    mutable IntrusiveList<CacheEntry, &CacheEntry::list_node> m_clean_list;
    mutable IntrusiveList<CacheEntry, &CacheEntry::list_node> m_dirty_list;
    KBuffer m_cached_block_data;
    KBuffer m_entries;
    bool m_dirty { false };
};

BlockBasedFS::BlockBasedFS(FileDescription& file_description)
    : FileBackedFS(file_description)
{
    VERIFY(file_description.file().is_seekable());
}

BlockBasedFS::~BlockBasedFS()
{
}

KResult BlockBasedFS::write_block(BlockIndex index, const UserOrKernelBuffer& data, size_t count, size_t offset, bool allow_cache)
{
    LOCKER(m_lock);
    VERIFY(m_logical_block_size);
    VERIFY(offset + count <= block_size());
    dbgln_if(BBFS_DEBUG, "BlockBasedFileSystem::write_block {}, size={}", index, count);

    if (!allow_cache) {
        flush_specific_block_if_needed(index);
        u32 base_offset = index.value() * block_size() + offset;
        file_description().seek(base_offset, SEEK_SET);
        auto nwritten = file_description().write(data, count);
        if (nwritten.is_error())
            return nwritten.error();
        VERIFY(nwritten.value() == count);
        return KSuccess;
    }

    auto& entry = cache().get(index);
    if (count < block_size()) {
        // Fill the cache first.
        auto result = read_block(index, nullptr, block_size());
        if (result.is_error())
            return result;
    }
    if (!data.read(entry.data + offset, count))
        return EFAULT;

    cache().mark_dirty(entry);
    entry.has_data = true;
    return KSuccess;
}

bool BlockBasedFS::raw_read(BlockIndex index, UserOrKernelBuffer& buffer)
{
    LOCKER(m_lock);
    u32 base_offset = index.value() * m_logical_block_size;
    file_description().seek(base_offset, SEEK_SET);
    auto nread = file_description().read(buffer, m_logical_block_size);
    VERIFY(!nread.is_error());
    VERIFY(nread.value() == m_logical_block_size);
    return true;
}

bool BlockBasedFS::raw_write(BlockIndex index, const UserOrKernelBuffer& buffer)
{
    LOCKER(m_lock);
    size_t base_offset = index.value() * m_logical_block_size;
    file_description().seek(base_offset, SEEK_SET);
    auto nwritten = file_description().write(buffer, m_logical_block_size);
    VERIFY(!nwritten.is_error());
    VERIFY(nwritten.value() == m_logical_block_size);
    return true;
}

bool BlockBasedFS::raw_read_blocks(BlockIndex index, size_t count, UserOrKernelBuffer& buffer)
{
    LOCKER(m_lock);
    auto current = buffer;
    for (unsigned block = index.value(); block < (index.value() + count); block++) {
        if (!raw_read(BlockIndex { block }, current))
            return false;
        current = current.offset(logical_block_size());
    }
    return true;
}

bool BlockBasedFS::raw_write_blocks(BlockIndex index, size_t count, const UserOrKernelBuffer& buffer)
{
    LOCKER(m_lock);
    auto current = buffer;
    for (unsigned block = index.value(); block < (index.value() + count); block++) {
        if (!raw_write(block, current))
            return false;
        current = current.offset(logical_block_size());
    }
    return true;
}

KResult BlockBasedFS::write_blocks(BlockIndex index, unsigned count, const UserOrKernelBuffer& data, bool allow_cache)
{
    LOCKER(m_lock);
    VERIFY(m_logical_block_size);
    dbgln_if(BBFS_DEBUG, "BlockBasedFileSystem::write_blocks {}, count={}", index, count);
    for (unsigned i = 0; i < count; ++i) {
        auto result = write_block(BlockIndex { index.value() + i }, data.offset(i * block_size()), block_size(), 0, allow_cache);
        if (result.is_error())
            return result;
    }
    return KSuccess;
}

KResult BlockBasedFS::read_block(BlockIndex index, UserOrKernelBuffer* buffer, size_t count, size_t offset, bool allow_cache) const
{
    LOCKER(m_lock);
    VERIFY(m_logical_block_size);
    VERIFY(offset + count <= block_size());
    dbgln_if(BBFS_DEBUG, "BlockBasedFileSystem::read_block {}", index);

    if (!allow_cache) {
        const_cast<BlockBasedFS*>(this)->flush_specific_block_if_needed(index);
        size_t base_offset = index.value() * block_size() + offset;
        file_description().seek(base_offset, SEEK_SET);
        auto nread = file_description().read(*buffer, count);
        if (nread.is_error())
            return nread.error();
        VERIFY(nread.value() == count);
        return KSuccess;
    }

    auto& entry = cache().get(index);
    if (!entry.has_data) {
        size_t base_offset = index.value() * block_size();
        file_description().seek(base_offset, SEEK_SET);
        auto entry_data_buffer = UserOrKernelBuffer::for_kernel_buffer(entry.data);
        auto nread = file_description().read(entry_data_buffer, block_size());
        if (nread.is_error())
            return nread.error();
        VERIFY(nread.value() == block_size());
        entry.has_data = true;
    }
    if (buffer && !buffer->write(entry.data + offset, count))
        return EFAULT;
    return KSuccess;
}

KResult BlockBasedFS::read_blocks(BlockIndex index, unsigned count, UserOrKernelBuffer& buffer, bool allow_cache) const
{
    LOCKER(m_lock);
    VERIFY(m_logical_block_size);
    if (!count)
        return EINVAL;
    if (count == 1)
        return read_block(index, &buffer, block_size(), 0, allow_cache);
    auto out = buffer;
    for (unsigned i = 0; i < count; ++i) {
        auto result = read_block(BlockIndex { index.value() + i }, &out, block_size(), 0, allow_cache);
        if (result.is_error())
            return result;
        out = out.offset(block_size());
    }

    return KSuccess;
}

void BlockBasedFS::flush_specific_block_if_needed(BlockIndex index)
{
    LOCKER(m_lock);
    if (!cache().is_dirty())
        return;
    Vector<CacheEntry*, 32> cleaned_entries;
    cache().for_each_dirty_entry([&](CacheEntry& entry) {
        if (entry.block_index != index) {
            size_t base_offset = entry.block_index.value() * block_size();
            file_description().seek(base_offset, SEEK_SET);
            // FIXME: Should this error path be surfaced somehow?
            auto entry_data_buffer = UserOrKernelBuffer::for_kernel_buffer(entry.data);
            [[maybe_unused]] auto rc = file_description().write(entry_data_buffer, block_size());
            cleaned_entries.append(&entry);
        }
    });
    // NOTE: We make a separate pass to mark entries clean since marking them clean
    //       moves them out of the dirty list which would disturb the iteration above.
    for (auto* entry : cleaned_entries)
        cache().mark_clean(*entry);
}

void BlockBasedFS::flush_writes_impl()
{
    LOCKER(m_lock);
    if (!cache().is_dirty())
        return;
    u32 count = 0;
    cache().for_each_dirty_entry([&](CacheEntry& entry) {
        u32 base_offset = entry.block_index.value() * block_size();
        file_description().seek(base_offset, SEEK_SET);
        // FIXME: Should this error path be surfaced somehow?
        auto entry_data_buffer = UserOrKernelBuffer::for_kernel_buffer(entry.data);
        [[maybe_unused]] auto rc = file_description().write(entry_data_buffer, block_size());
        ++count;
    });
    cache().mark_all_clean();
    dbgln("{}: Flushed {} blocks to disk", class_name(), count);
}

void BlockBasedFS::flush_writes()
{
    flush_writes_impl();
}

DiskCache& BlockBasedFS::cache() const
{
    if (!m_cache)
        m_cache = make<DiskCache>(const_cast<BlockBasedFS&>(*this));
    return *m_cache;
}

}
