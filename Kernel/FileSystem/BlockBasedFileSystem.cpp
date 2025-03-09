/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntrusiveList.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/BlockBasedFileSystem.h>
#include <Kernel/Tasks/Process.h>

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
        : m_cached_block_data(move(cached_block_data))
        , m_entries(move(entries_buffer))
    {
        for (size_t i = 0; i < EntryCount; ++i) {
            entries()[i].data = m_cached_block_data->data() + i * fs.logical_block_size();
            m_clean_list.append(entries()[i]);
        }
    }

    ~DiskCache() = default;

    bool is_dirty() const { return !m_dirty_list.is_empty(); }
    bool entry_is_dirty(CacheEntry const& entry) const { return m_dirty_list.contains(entry); }

    void mark_all_clean()
    {
        while (auto* entry = m_dirty_list.first())
            m_clean_list.prepend(*entry);
    }

    void mark_dirty(CacheEntry& entry)
    {
        m_dirty_list.prepend(entry);
    }

    void mark_clean(CacheEntry& entry)
    {
        m_clean_list.prepend(entry);
    }

    CacheEntry* get(BlockBasedFileSystem::BlockIndex block_index) const
    {
        auto it = m_hash.find(block_index);
        if (it == m_hash.end())
            return nullptr;
        auto& entry = const_cast<CacheEntry&>(*it->value);
        VERIFY(entry.block_index == block_index);
        if (!entry_is_dirty(entry) && (m_clean_list.first() != &entry)) {
            // Cache hit! Promote the entry to the front of the list.
            m_clean_list.prepend(entry);
        }
        return &entry;
    }

    ErrorOr<CacheEntry*> ensure(BlockBasedFileSystem::BlockIndex block_index, BlockBasedFileSystem& fs) const
    {
        if (auto* entry = get(block_index))
            return entry;

        if (m_clean_list.is_empty()) {
            // Not a single clean entry! Flush writes and try again.
            // NOTE: We want to make sure we only call FileBackedFileSystem flush here,
            //       not some FileBackedFileSystem subclass flush!
            fs.flush_writes_impl();
            return ensure(block_index, fs);
        }

        VERIFY(m_clean_list.last());
        auto& new_entry = *m_clean_list.last();
        m_clean_list.prepend(new_entry);

        m_hash.remove(new_entry.block_index);
        TRY(m_hash.try_set(block_index, &new_entry));

        new_entry.block_index = block_index;
        new_entry.has_data = false;

        return &new_entry;
    }

    CacheEntry const* entries() const { return (CacheEntry const*)m_entries->data(); }
    CacheEntry* entries() { return (CacheEntry*)m_entries->data(); }

    template<typename Callback>
    void for_each_dirty_entry(Callback callback)
    {
        for (auto& entry : m_dirty_list)
            callback(entry);
    }

private:
    NonnullOwnPtr<KBuffer> m_cached_block_data;

    // NOTE: m_entries must be declared before m_dirty_list and m_clean_list because their entries are allocated from it.
    // We need to ensure that the destructors of m_dirty_list and m_clean_list are called before m_entries is destroyed.
    NonnullOwnPtr<KBuffer> m_entries;
    mutable IntrusiveList<&CacheEntry::list_node> m_dirty_list;
    mutable IntrusiveList<&CacheEntry::list_node> m_clean_list;
    mutable HashMap<BlockBasedFileSystem::BlockIndex, CacheEntry*> m_hash;
};

BlockBasedFileSystem::BlockBasedFileSystem(OpenFileDescription& file_description)
    : FileBackedFileSystem(file_description)
{
    VERIFY(file_description.file().is_seekable());
}

BlockBasedFileSystem::~BlockBasedFileSystem() = default;

ErrorOr<void> BlockBasedFileSystem::initialize_while_locked()
{
    VERIFY(m_lock.is_locked());
    VERIFY(!is_initialized_while_locked());
    VERIFY(logical_block_size() != 0);
    auto cached_block_data = TRY(KBuffer::try_create_with_size("BlockBasedFS: Cache blocks"sv, DiskCache::EntryCount * logical_block_size()));
    auto entries_data = TRY(KBuffer::try_create_with_size("BlockBasedFS: Cache entries"sv, DiskCache::EntryCount * sizeof(CacheEntry)));
    auto disk_cache = TRY(adopt_nonnull_own_or_enomem(new (nothrow) DiskCache(*this, move(cached_block_data), move(entries_data))));

    m_cache.with_exclusive([&](auto& cache) {
        cache = move(disk_cache);
    });
    return {};
}

ErrorOr<void> BlockBasedFileSystem::write_block(BlockIndex index, UserOrKernelBuffer const& data, size_t count, u64 offset, bool allow_cache)
{
    VERIFY(m_device_block_size);
    VERIFY(offset + count <= logical_block_size());
    dbgln_if(BBFS_DEBUG, "BlockBasedFileSystem::write_block {}, size={}", index, count);

    // NOTE: We copy the `data` to write into a local buffer before taking the cache lock.
    //       This makes sure any page faults caused by accessing the data will occur before
    //       we tie down the cache.
    auto buffered_data = TRY(ByteBuffer::create_uninitialized(count));

    TRY(data.read(buffered_data.bytes()));

    return m_cache.with_exclusive([&](auto& cache) -> ErrorOr<void> {
        if (!allow_cache) {
            flush_specific_block_if_needed(index);
            u64 base_offset = index.value() * logical_block_size() + offset;
            auto nwritten = TRY(file_description().write(base_offset, data, count));
            VERIFY(nwritten == count);
            return {};
        }

        auto entry = TRY(cache->ensure(index, *this));
        if (count < logical_block_size()) {
            // Fill the cache first.
            TRY(read_block(index, nullptr, logical_block_size()));
        }
        memcpy(entry->data + offset, buffered_data.data(), count);

        cache->mark_dirty(*entry);
        entry->has_data = true;
        return {};
    });
}

ErrorOr<void> BlockBasedFileSystem::raw_read(BlockIndex index, UserOrKernelBuffer& buffer)
{
    auto base_offset = index.value() * m_device_block_size;
    auto nread = TRY(file_description().read(buffer, base_offset, m_device_block_size));
    VERIFY(nread == m_device_block_size);
    return {};
}

ErrorOr<void> BlockBasedFileSystem::raw_write(BlockIndex index, UserOrKernelBuffer const& buffer)
{
    auto base_offset = index.value() * m_device_block_size;
    auto nwritten = TRY(file_description().write(base_offset, buffer, m_device_block_size));
    VERIFY(nwritten == m_device_block_size);
    return {};
}

ErrorOr<void> BlockBasedFileSystem::raw_read_blocks(BlockIndex index, size_t count, UserOrKernelBuffer& buffer)
{
    auto current = buffer;
    for (auto block = index.value(); block < (index.value() + count); block++) {
        TRY(raw_read(BlockIndex { block }, current));
        current = current.offset(device_block_size());
    }
    return {};
}

ErrorOr<void> BlockBasedFileSystem::raw_write_blocks(BlockIndex index, size_t count, UserOrKernelBuffer const& buffer)
{
    auto current = buffer;
    for (auto block = index.value(); block < (index.value() + count); block++) {
        TRY(raw_write(block, current));
        current = current.offset(device_block_size());
    }
    return {};
}

ErrorOr<void> BlockBasedFileSystem::write_blocks(BlockIndex index, unsigned count, UserOrKernelBuffer const& data, bool allow_cache)
{
    VERIFY(m_device_block_size);
    dbgln_if(BBFS_DEBUG, "BlockBasedFileSystem::write_blocks {}, count={}", index, count);
    for (unsigned i = 0; i < count; ++i) {
        TRY(write_block(BlockIndex { index.value() + i }, data.offset(i * logical_block_size()), logical_block_size(), 0, allow_cache));
    }
    return {};
}

ErrorOr<void> BlockBasedFileSystem::read_block(BlockIndex index, UserOrKernelBuffer* buffer, size_t count, u64 offset, bool allow_cache) const
{
    VERIFY(m_device_block_size);
    VERIFY(offset + count <= logical_block_size());
    dbgln_if(BBFS_DEBUG, "BlockBasedFileSystem::read_block {}", index);

    return m_cache.with_exclusive([&](auto& cache) -> ErrorOr<void> {
        if (!allow_cache) {
            const_cast<BlockBasedFileSystem*>(this)->flush_specific_block_if_needed(index);
            u64 base_offset = index.value() * logical_block_size() + offset;
            auto nread = TRY(file_description().read(*buffer, base_offset, count));
            VERIFY(nread == count);
            return {};
        }

        auto* entry = TRY(cache->ensure(index, const_cast<BlockBasedFileSystem&>(*this)));
        if (!entry->has_data) {
            auto base_offset = index.value() * logical_block_size();
            auto entry_data_buffer = UserOrKernelBuffer::for_kernel_buffer(entry->data);
            auto nread = TRY(file_description().read(entry_data_buffer, base_offset, logical_block_size()));
            VERIFY(nread == logical_block_size());
            entry->has_data = true;
        }
        if (buffer)
            TRY(buffer->write(entry->data + offset, count));
        return {};
    });
}

ErrorOr<void> BlockBasedFileSystem::read_blocks(BlockIndex index, unsigned count, UserOrKernelBuffer& buffer, bool allow_cache) const
{
    VERIFY(m_device_block_size);
    if (!count)
        return EINVAL;
    if (count == 1)
        return read_block(index, &buffer, logical_block_size(), 0, allow_cache);
    auto out = buffer;
    for (unsigned i = 0; i < count; ++i) {
        TRY(read_block(BlockIndex { index.value() + i }, &out, logical_block_size(), 0, allow_cache));
        out = out.offset(logical_block_size());
    }

    return {};
}

void BlockBasedFileSystem::flush_specific_block_if_needed(BlockIndex index)
{
    m_cache.with_exclusive([&](auto& cache) {
        if (!cache->is_dirty())
            return;
        auto* entry = cache->get(index);
        if (!entry)
            return;
        if (!cache->entry_is_dirty(*entry))
            return;
        size_t base_offset = entry->block_index.value() * logical_block_size();
        auto entry_data_buffer = UserOrKernelBuffer::for_kernel_buffer(entry->data);
        (void)file_description().write(base_offset, entry_data_buffer, logical_block_size());
    });
}

void BlockBasedFileSystem::flush_writes_impl()
{
    size_t count = 0;
    m_cache.with_exclusive([&](auto& cache) {
        if (!cache->is_dirty())
            return;
        cache->for_each_dirty_entry([&](CacheEntry& entry) {
            auto base_offset = entry.block_index.value() * logical_block_size();
            auto entry_data_buffer = UserOrKernelBuffer::for_kernel_buffer(entry.data);
            [[maybe_unused]] auto rc = file_description().write(base_offset, entry_data_buffer, logical_block_size());
            ++count;
        });
        cache->mark_all_clean();
        dbgln("{}: Flushed {} blocks to disk", class_name(), count);
    });
}

ErrorOr<void> BlockBasedFileSystem::flush_writes()
{
    flush_writes_impl();
    return {};
}

}
