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
    explicit DiskCache(size_t block_size)
        : m_cached_block_data(KBuffer::create_with_size(m_entry_count * block_size))
    {
        m_entries = (CacheEntry*)kmalloc_eternal(m_entry_count * sizeof(CacheEntry));
        for (size_t i = 0; i < m_entry_count; ++i) {
            m_entries[i].data = m_cached_block_data.data() + i * block_size;
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
            auto& entry = m_entries[i];
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
        // FIXME: What if every single entry was dirty though :(
        ASSERT(oldest_clean_entry);

        // Replace the oldest clean entry.
        auto& new_entry = *oldest_clean_entry;
        new_entry.timestamp = now;
        new_entry.block_index = block_index;
        new_entry.has_data = false;
        new_entry.is_dirty = false;
        return new_entry;
    }

    template<typename Callback>
    void for_each_entry(Callback callback)
    {
        for (size_t i = 0; i < m_entry_count; ++i)
            callback(m_entries[i]);
    }

    size_t m_entry_count { 10000 };
    KBuffer m_cached_block_data;
    CacheEntry* m_entries { nullptr };
    bool m_dirty { false };
};

DiskBackedFS::DiskBackedFS(NonnullRefPtr<DiskDevice>&& device)
    : m_device(move(device))
{
}

DiskBackedFS::~DiskBackedFS()
{
}

bool DiskBackedFS::write_block(unsigned index, const ByteBuffer& data)
{
#ifdef DBFS_DEBUG
    kprintf("DiskBackedFileSystem::write_block %u, size=%u\n", index, data.size());
#endif
    ASSERT(data.size() == block_size());

    auto& entry = cache().get(index);
    memcpy(entry.data, data.data(), data.size());
    entry.is_dirty = true;
    entry.has_data = true;

    cache().set_dirty(true);
    return true;
}

bool DiskBackedFS::write_blocks(unsigned index, unsigned count, const ByteBuffer& data)
{
#ifdef DBFS_DEBUG
    kprintf("DiskBackedFileSystem::write_blocks %u x%u\n", index, count);
#endif
    for (unsigned i = 0; i < count; ++i)
        write_block(index + i, data.slice(i * block_size(), block_size()));
    return true;
}

ByteBuffer DiskBackedFS::read_block(unsigned index) const
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
    return ByteBuffer::copy(entry.data, block_size());
}

ByteBuffer DiskBackedFS::read_blocks(unsigned index, unsigned count) const
{
    if (!count)
        return nullptr;
    if (count == 1)
        return read_block(index);
    auto blocks = ByteBuffer::create_uninitialized(count * block_size());
    u8* out = blocks.data();

    for (unsigned i = 0; i < count; ++i) {
        auto block = read_block(index + i);
        if (!block)
            return nullptr;
        memcpy(out, block.data(), block.size());
        out += block_size();
    }

    return blocks;
}

void DiskBackedFS::flush_writes()
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
    dbg() << class_name() << ": " << "Flushed " << count << " blocks to disk";
}

DiskCache& DiskBackedFS::cache() const
{
    if (!m_cache)
        m_cache = make<DiskCache>(block_size());
    return *m_cache;
}
