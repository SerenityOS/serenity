#include "DiskBackedFileSystem.h"
#include "i386.h"
#include <AK/InlineLRUCache.h>

//#define DBFS_DEBUG

struct BlockIdentifier {
    unsigned fsid { 0 };
    unsigned index { 0 };

    bool operator==(const BlockIdentifier& other) const { return fsid == other.fsid && index == other.index; }
};

namespace AK {

template<>
struct Traits<BlockIdentifier> {
    static unsigned hash(const BlockIdentifier& block_id) { return pair_int_hash(block_id.fsid, block_id.index); }
    static void dump(const BlockIdentifier& block_id) { kprintf("[block %02u:%08u]", block_id.fsid, block_id.index); }
};

}

class CachedBlock : public InlineLinkedListNode<CachedBlock> {
public:
    CachedBlock(const BlockIdentifier& block_id, const ByteBuffer& buffer)
        : m_key(block_id)
        , m_buffer(buffer)
    {
    }

    BlockIdentifier m_key;
    CachedBlock* m_next { nullptr };
    CachedBlock* m_prev { nullptr };

    ByteBuffer m_buffer;
};

Lockable<InlineLRUCache<BlockIdentifier, CachedBlock>>& block_cache()
{
    static Lockable<InlineLRUCache<BlockIdentifier, CachedBlock>>* s_cache;
    if (!s_cache)
        s_cache = new Lockable<InlineLRUCache<BlockIdentifier, CachedBlock>>;
    return *s_cache;
}

DiskBackedFS::DiskBackedFS(RetainPtr<DiskDevice>&& device)
    : m_device(move(device))
{
    ASSERT(m_device);
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

    {
        LOCKER(block_cache().lock());
        if (auto* cached_block = block_cache().resource().get({ fsid(), index }))
            cached_block->m_buffer = data;
    }
    DiskOffset base_offset = static_cast<DiskOffset>(index) * static_cast<DiskOffset>(block_size());
    return device().write(base_offset, block_size(), data.pointer());
}

bool DiskBackedFS::write_blocks(unsigned index, unsigned count, const ByteBuffer& data)
{
#ifdef DBFS_DEBUG
    kprintf("DiskBackedFileSystem::write_blocks %u x%u\n", index, count);
#endif
    // FIXME: Maybe reorder this so we send out the write commands before updating cache?
    {
        LOCKER(block_cache().lock());
        for (unsigned i = 0; i < count; ++i) {
            if (auto* cached_block = block_cache().resource().get({ fsid(), index + i }))
                cached_block->m_buffer = data.slice(i * block_size(), block_size());
        }
    }
    DiskOffset base_offset = static_cast<DiskOffset>(index) * static_cast<DiskOffset>(block_size());
    return device().write(base_offset, count * block_size(), data.pointer());
}

ByteBuffer DiskBackedFS::read_block(unsigned index) const
{
#ifdef DBFS_DEBUG
    kprintf("DiskBackedFileSystem::read_block %u\n", index);
#endif
    {
        LOCKER(block_cache().lock());
        if (auto* cached_block = block_cache().resource().get({ fsid(), index }))
            return cached_block->m_buffer;
    }

    auto buffer = ByteBuffer::create_uninitialized(block_size());
    //kprintf("created block buffer with size %u\n", block_size());
    DiskOffset base_offset = static_cast<DiskOffset>(index) * static_cast<DiskOffset>(block_size());
    auto* buffer_pointer = buffer.pointer();
    bool success = device().read(base_offset, block_size(), buffer_pointer);
    ASSERT(success);
    ASSERT(buffer.size() == block_size());
    {
        LOCKER(block_cache().lock());
        block_cache().resource().put({ fsid(), index }, CachedBlock({ fsid(), index }, buffer));
    }
    return buffer;
}

ByteBuffer DiskBackedFS::read_blocks(unsigned index, unsigned count) const
{
    if (!count)
        return nullptr;
    if (count == 1)
        return read_block(index);
    auto blocks = ByteBuffer::create_uninitialized(count * block_size());
    byte* out = blocks.pointer();

    for (unsigned i = 0; i < count; ++i) {
        auto block = read_block(index + i);
        if (!block)
            return nullptr;
        memcpy(out, block.pointer(), block.size());
        out += block_size();
    }

    return blocks;
}

void DiskBackedFS::set_block_size(unsigned block_size)
{
    if (block_size == m_block_size)
        return;
    m_block_size = block_size;
}
