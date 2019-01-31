#include "DiskBackedFileSystem.h"
#include "i386.h"

//#define DBFS_DEBUG

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
    DiskOffset base_offset = static_cast<DiskOffset>(index) * static_cast<DiskOffset>(block_size());
    return device().write(base_offset, block_size(), data.pointer());
}

bool DiskBackedFS::write_blocks(unsigned index, unsigned count, const ByteBuffer& data)
{
#ifdef DBFS_DEBUG
    kprintf("DiskBackedFileSystem::write_blocks %u x%u\n", index, count);
#endif
    DiskOffset base_offset = static_cast<DiskOffset>(index) * static_cast<DiskOffset>(block_size());
    return device().write(base_offset, count * block_size(), data.pointer());
}

ByteBuffer DiskBackedFS::read_block(unsigned index) const
{
#ifdef DBFS_DEBUG
    kprintf("DiskBackedFileSystem::read_block %u\n", index);
#endif
    auto buffer = ByteBuffer::create_uninitialized(block_size());
    //kprintf("created block buffer with size %u\n", block_size());
    DiskOffset base_offset = static_cast<DiskOffset>(index) * static_cast<DiskOffset>(block_size());
    auto* buffer_pointer = buffer.pointer();
    bool success = device().read(base_offset, block_size(), buffer_pointer);
    ASSERT(success);
    ASSERT(buffer.size() == block_size());
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
