/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntrusiveList.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/BlockBasedFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

struct CacheEntry {
    IntrusiveListNode<CacheEntry> list_node;
    BlockBasedFileSystem::BlockIndex block_index { 0 };
    u8* data { nullptr };
    bool has_data { false };
};

class DiskCache {
public:
    static constexpr size_t EntryCount = 10000;
    explicit DiskCache(BlockBasedFileSystem& fs, NonnullOwnPtr<KBuffer> cached_block_data, NonnullOwnPtr<KBuffer> entries_buffer)
        : m_fs(fs)
        , m_cached_block_data(move(cached_block_data))
        , m_entries(move(entries_buffer))
    {
        for (size_t i = 0; i < EntryCount; ++i) {
            entries()[i].data = m_cached_block_data->data() + i * m_fs.block_size();
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

    CacheEntry& get(BlockBasedFileSystem::BlockIndex block_index) const
    {
        if (auto it = m_hash.find(block_index); it != m_hash.end()) {
            auto& entry = const_cast<CacheEntry&>(*it->value);
            VERIFY(entry.block_index == block_index);
            return entry;
        }

        if (m_clean_list.is_empty()) {
            // Not a single clean entry! Flush writes and try again.
            // NOTE: We want to make sure we only call FileBackedFileSystem flush here,
            //       not some FileBackedFileSystem subclass flush!
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

    const CacheEntry* entries() const { return (const CacheEntry*)m_entries->data(); }
    CacheEntry* entries() { return (CacheEntry*)m_entries->data(); }

    template<typename Callback>
    void for_each_dirty_entry(Callback callback)
    {
        for (auto& entry : m_dirty_list)
            callback(entry);
    }

private:
    BlockBasedFileSystem& m_fs;
    mutable HashMap<BlockBasedFileSystem::BlockIndex, CacheEntry*> m_hash;
    mutable IntrusiveList<&CacheEntry::list_node> m_clean_list;
    mutable IntrusiveList<&CacheEntry::list_node> m_dirty_list;
    NonnullOwnPtr<KBuffer> m_cached_block_data;
    NonnullOwnPtr<KBuffer> m_entries;
    bool m_dirty { false };
};

BlockBasedFileSystem::BlockBasedFileSystem(OpenFileDescription& file_description)
    : FileBackedFileSystem(file_description)
{
    VERIFY(file_description.file().is_seekable());
}

BlockBasedFileSystem::~BlockBasedFileSystem()
{
}

KResult BlockBasedFileSystem::initialize()
{
    VERIFY(block_size() != 0);
    auto cached_block_data = TRY(KBuffer::try_create_with_size(DiskCache::EntryCount * block_size()));
    auto entries_data = TRY(KBuffer::try_create_with_size(DiskCache::EntryCount * sizeof(CacheEntry)));
    auto disk_cache = TRY(adopt_nonnull_own_or_enomem(new (nothrow) DiskCache(*this, move(cached_block_data), move(entries_data))));

    m_cache.with_exclusive([&](auto& cache) {
        cache = move(disk_cache);
    });
    return KSuccess;
}

KResult BlockBasedFileSystem::write_block(BlockIndex index, const UserOrKernelBuffer& data, size_t count, size_t offset, bool allow_cache)
{
    VERIFY(m_logical_block_size);
    VERIFY(offset + count <= block_size());
    dbgln_if(BBFS_DEBUG, "BlockBasedFileSystem::write_block {}, size={}", index, count);

    return m_cache.with_exclusive([&](auto& cache) -> KResult {
        if (!allow_cache) {
            flush_specific_block_if_needed(index);
            auto base_offset = index.value() * block_size() + offset;
            auto nwritten = TRY(file_description().write(base_offset, data, count));
            VERIFY(nwritten == count);
            return KSuccess;
        }

        auto& entry = cache->get(index);
        if (count < block_size()) {
            // Fill the cache first.
            TRY(read_block(index, nullptr, block_size()));
        }
        TRY(data.read(entry.data + offset, count));

        cache->mark_dirty(entry);
        entry.has_data = true;
        return KSuccess;
    });
}

bool BlockBasedFileSystem::raw_read(BlockIndex index, UserOrKernelBuffer& buffer)
{
    auto base_offset = index.value() * m_logical_block_size;
    auto nread = file_description().read(buffer, base_offset, m_logical_block_size);
    VERIFY(!nread.is_error());
    VERIFY(nread.value() == m_logical_block_size);
    return true;
}

bool BlockBasedFileSystem::raw_write(BlockIndex index, const UserOrKernelBuffer& buffer)
{
    auto base_offset = index.value() * m_logical_block_size;
    auto nwritten = file_description().write(base_offset, buffer, m_logical_block_size);
    VERIFY(!nwritten.is_error());
    VERIFY(nwritten.value() == m_logical_block_size);
    return true;
}

bool BlockBasedFileSystem::raw_read_blocks(BlockIndex index, size_t count, UserOrKernelBuffer& buffer)
{
    auto current = buffer;
    for (auto block = index.value(); block < (index.value() + count); block++) {
        if (!raw_read(BlockIndex { block }, current))
            return false;
        current = current.offset(logical_block_size());
    }
    return true;
}

bool BlockBasedFileSystem::raw_write_blocks(BlockIndex index, size_t count, const UserOrKernelBuffer& buffer)
{
    auto current = buffer;
    for (auto block = index.value(); block < (index.value() + count); block++) {
        if (!raw_write(block, current))
            return false;
        current = current.offset(logical_block_size());
    }
    return true;
}

KResult BlockBasedFileSystem::write_blocks(BlockIndex index, unsigned count, const UserOrKernelBuffer& data, bool allow_cache)
{
    VERIFY(m_logical_block_size);
    dbgln_if(BBFS_DEBUG, "BlockBasedFileSystem::write_blocks {}, count={}", index, count);
    for (unsigned i = 0; i < count; ++i) {
        TRY(write_block(BlockIndex { index.value() + i }, data.offset(i * block_size()), block_size(), 0, allow_cache));
    }
    return KSuccess;
}

KResult BlockBasedFileSystem::read_block(BlockIndex index, UserOrKernelBuffer* buffer, size_t count, size_t offset, bool allow_cache) const
{
    VERIFY(m_logical_block_size);
    VERIFY(offset + count <= block_size());
    dbgln_if(BBFS_DEBUG, "BlockBasedFileSystem::read_block {}", index);

    return m_cache.with_exclusive([&](auto& cache) -> KResult {
        if (!allow_cache) {
            const_cast<BlockBasedFileSystem*>(this)->flush_specific_block_if_needed(index);
            auto base_offset = index.value() * block_size() + offset;
            auto nread = TRY(file_description().read(*buffer, base_offset, count));
            VERIFY(nread == count);
            return KSuccess;
        }

        auto& entry = cache->get(index);
        if (!entry.has_data) {
            auto base_offset = index.value() * block_size();
            auto entry_data_buffer = UserOrKernelBuffer::for_kernel_buffer(entry.data);
            auto nread = TRY(file_description().read(entry_data_buffer, base_offset, block_size()));
            VERIFY(nread == block_size());
            entry.has_data = true;
        }
        if (buffer)
            TRY(buffer->write(entry.data + offset, count));
        return KSuccess;
    });
}

KResult BlockBasedFileSystem::read_blocks(BlockIndex index, unsigned count, UserOrKernelBuffer& buffer, bool allow_cache) const
{
    VERIFY(m_logical_block_size);
    if (!count)
        return EINVAL;
    if (count == 1)
        return read_block(index, &buffer, block_size(), 0, allow_cache);
    auto out = buffer;
    for (unsigned i = 0; i < count; ++i) {
        TRY(read_block(BlockIndex { index.value() + i }, &out, block_size(), 0, allow_cache));
        out = out.offset(block_size());
    }

    return KSuccess;
}

void BlockBasedFileSystem::flush_specific_block_if_needed(BlockIndex index)
{
    m_cache.with_exclusive([&](auto& cache) {
        if (!cache->is_dirty())
            return;
        Vector<CacheEntry*, 32> cleaned_entries;
        cache->for_each_dirty_entry([&](CacheEntry& entry) {
            if (entry.block_index != index) {
                size_t base_offset = entry.block_index.value() * block_size();
                auto entry_data_buffer = UserOrKernelBuffer::for_kernel_buffer(entry.data);
                [[maybe_unused]] auto rc = file_description().write(base_offset, entry_data_buffer, block_size());
                cleaned_entries.append(&entry);
            }
        });
        // NOTE: We make a separate pass to mark entries clean since marking them clean
        //       moves them out of the dirty list which would disturb the iteration above.
        for (auto* entry : cleaned_entries)
            cache->mark_clean(*entry);
    });
}

void BlockBasedFileSystem::flush_writes_impl()
{
    size_t count = 0;
    m_cache.with_exclusive([&](auto& cache) {
        if (!cache->is_dirty())
            return;
        cache->for_each_dirty_entry([&](CacheEntry& entry) {
            auto base_offset = entry.block_index.value() * block_size();
            auto entry_data_buffer = UserOrKernelBuffer::for_kernel_buffer(entry.data);
            if (auto rc = file_description().write(base_offset, entry_data_buffer, block_size()); rc.is_error()) {
                if (rc.error() == ErrnoCode::EIO)
                    return IterationDecision::Break;
            }
            ++count;
            return IterationDecision::Continue;
        });
        cache->mark_all_clean();
        dbgln("{}: Flushed {} blocks to disk", class_name(), count);
    });
}

void BlockBasedFileSystem::flush_writes()
{
    flush_writes_impl();
}

}
