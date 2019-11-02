#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/FileSystem/DiskBackedFileSystem.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Process.h>

//#define DBFS_DEBUG

struct CacheEntry {
    u32 timestamp { 0 };
    u32 block_index { 0 };
    u8* data { nullptr };
    bool has_data { false };
    bool is_dirty { false };
};

class DiskCache {
public:
    explicit DiskCache(DiskBackedFS& fs)
        : m_fs(fs)
        , m_cached_block_data(KBuffer::create_with_size(m_entry_count * m_fs.block_size()))
        , m_entries(KBuffer::create_with_size(m_entry_count * sizeof(CacheEntry)))
    {
        for (size_t i = 0; i < m_entry_count; ++i) {
            entries()[i].data = m_cached_block_data.data() + i * m_fs.block_size();
        }
    }

    ~DiskCache() {}

    bool is_dirty() const { return m_dirty; }
    void set_dirty(bool b) { m_dirty = b; }

    CacheEntry& get(u32 block_index) const
    {
        auto now = kgettimeofday().tv_sec;

        CacheEntry* oldest_clean_entry = nullptr;
        for (size_t i = 0; i < m_entry_count; ++i) {
            auto& entry = const_cast<CacheEntry&>(entries()[i]);
            if (entry.block_index == block_index) {
                entry.timestamp = now;
                return entry;
            }
            if (!entry.is_dirty) {
                if (!oldest_clean_entry)
                    oldest_clean_entry = &entry;
                else if (entry.timestamp < oldest_clean_entry->timestamp)
                    oldest_clean_entry = &entry;
            }
        }
        if (!oldest_clean_entry) {
            // Not a single clean entry! Flush writes and try again.
            // NOTE: We want to make sure we only call DiskBackedFS flush here,
            //       not some DiskBackedFS subclass flush!
            m_fs.flush_writes_impl();
            return get(block_index);
        }

        // Replace the oldest clean entry.
        auto& new_entry = *oldest_clean_entry;
        new_entry.timestamp = now;
        new_entry.block_index = block_index;
        new_entry.has_data = false;
        new_entry.is_dirty = false;
        return new_entry;
    }

    const CacheEntry* entries() const { return (const CacheEntry*)m_entries.data(); }
    CacheEntry* entries() { return (CacheEntry*)m_entries.data(); }

    template<typename Callback>
    void for_each_entry(Callback callback)
    {
        for (size_t i = 0; i < m_entry_count; ++i)
            callback(entries()[i]);
    }

private:
    DiskBackedFS& m_fs;
    size_t m_entry_count { 10000 };
    KBuffer m_cached_block_data;
    KBuffer m_entries;
    bool m_dirty { false };
};

DiskBackedFS::DiskBackedFS(NonnullRefPtr<DiskDevice>&& device)
    : m_device(move(device))
{
}

DiskBackedFS::~DiskBackedFS()
{
}

bool DiskBackedFS::write_block(unsigned index, const u8* data)
{
#ifdef DBFS_DEBUG
    kprintf("DiskBackedFileSystem::write_block %u, size=%u\n", index, data.size());
#endif
    auto& entry = cache().get(index);
    memcpy(entry.data, data, block_size());
    entry.is_dirty = true;
    entry.has_data = true;

    cache().set_dirty(true);
    return true;
}

bool DiskBackedFS::write_blocks(unsigned index, unsigned count, const u8* data)
{
#ifdef DBFS_DEBUG
    kprintf("DiskBackedFileSystem::write_blocks %u x%u\n", index, count);
#endif
    for (unsigned i = 0; i < count; ++i)
        write_block(index + i, data + i * block_size());
    return true;
}

bool DiskBackedFS::read_block(unsigned index, u8* buffer) const
{
#ifdef DBFS_DEBUG
    kprintf("DiskBackedFileSystem::read_block %u\n", index);
#endif

    auto& entry = cache().get(index);
    if (!entry.has_data) {
        DiskOffset base_offset = static_cast<DiskOffset>(index) * static_cast<DiskOffset>(block_size());
        bool success = device().read(base_offset, block_size(), entry.data);
        entry.has_data = true;
        ASSERT(success);
    }
    memcpy(buffer, entry.data, block_size());
    return true;
}

bool DiskBackedFS::read_blocks(unsigned index, unsigned count, u8* buffer) const
{
    if (!count)
        return false;
    if (count == 1)
        return read_block(index, buffer);
    u8* out = buffer;

    for (unsigned i = 0; i < count; ++i) {
        if (!read_block(index + i, out))
            return false;
        out += block_size();
    }

    return true;
}

void DiskBackedFS::flush_writes_impl()
{
    LOCKER(m_lock);
    if (!cache().is_dirty())
        return;
    u32 count = 0;
    cache().for_each_entry([&](CacheEntry& entry) {
        if (!entry.is_dirty)
            return;
        DiskOffset base_offset = static_cast<DiskOffset>(entry.block_index) * static_cast<DiskOffset>(block_size());
        device().write(base_offset, block_size(), entry.data);
        ++count;
        entry.is_dirty = false;
    });
    cache().set_dirty(false);
    dbg() << class_name() << ": Flushed " << count << " blocks to disk";
}

void DiskBackedFS::flush_writes()
{
    flush_writes_impl();
}

DiskCache& DiskBackedFS::cache() const
{
    if (!m_cache)
        m_cache = make<DiskCache>(const_cast<DiskBackedFS&>(*this));
    return *m_cache;
}
