#include "DiskBackedFileSystem.h"

//#define DBFS_DEBUG

DiskBackedFileSystem::DiskBackedFileSystem(RetainPtr<DiskDevice>&& device)
    : m_device(move(device))
{
    ASSERT(m_device);
}

DiskBackedFileSystem::~DiskBackedFileSystem()
{
}

bool DiskBackedFileSystem::writeBlock(unsigned index, const ByteBuffer& data)
{
    ASSERT(data.size() == blockSize());
#ifdef DBFS_DEBUG
    printf("DiskBackedFileSystem::writeBlock %u\n", index);
#endif
    DiskOffset baseOffset = static_cast<DiskOffset>(index) * static_cast<DiskOffset>(blockSize());
    return device().write(baseOffset, blockSize(), data.pointer());
}

bool DiskBackedFileSystem::writeBlocks(unsigned index, unsigned count, const ByteBuffer& data)
{
#ifdef DBFS_DEBUG
    printf("DiskBackedFileSystem::writeBlocks %u x%u\n", index, count);
#endif
    DiskOffset baseOffset = static_cast<DiskOffset>(index) * static_cast<DiskOffset>(blockSize());
    return device().write(baseOffset, count * blockSize(), data.pointer());
}

ByteBuffer DiskBackedFileSystem::readBlock(unsigned index) const
{
#ifdef DBFS_DEBUG
    printf("DiskBackedFileSystem::readBlock %u\n", index);
#endif
    auto buffer = ByteBuffer::createUninitialized(blockSize());
    DiskOffset baseOffset = static_cast<DiskOffset>(index) * static_cast<DiskOffset>(blockSize());
    auto* bufferPointer = buffer.pointer();
    device().read(baseOffset, blockSize(), bufferPointer);
    ASSERT(buffer.size() == blockSize());
    return buffer;
}

ByteBuffer DiskBackedFileSystem::readBlocks(unsigned index, unsigned count) const
{
    if (!count)
        return nullptr;
    if (count == 1)
        return readBlock(index);
    auto blocks = ByteBuffer::createUninitialized(count * blockSize());
    byte* out = blocks.pointer();

    for (unsigned i = 0; i < count; ++i) {
        auto block = readBlock(index + i);
        if (!block)
            return nullptr;
        memcpy(out, block.pointer(), block.size());
        out += blockSize();
    }

    return blocks;
}

void DiskBackedFileSystem::setBlockSize(unsigned blockSize)
{
    if (blockSize == m_blockSize)
        return;
    m_blockSize = blockSize;
    invalidateCaches();
}

void DiskBackedFileSystem::invalidateCaches()
{
    // FIXME: Implement block cache.
}
