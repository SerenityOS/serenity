#include "DiskBackedFileSystem.h"

#ifdef SERENITY
#include "i386.h"
#else
typedef int InterruptDisabler;
#endif

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
    kprintf("DiskBackedFileSystem::writeBlock %u\n", index);
#endif
    DiskOffset baseOffset = static_cast<DiskOffset>(index) * static_cast<DiskOffset>(blockSize());
    return device().write(baseOffset, blockSize(), data.pointer());
}

bool DiskBackedFileSystem::writeBlocks(unsigned index, unsigned count, const ByteBuffer& data)
{
#ifdef DBFS_DEBUG
    kprintf("DiskBackedFileSystem::writeBlocks %u x%u\n", index, count);
#endif
    DiskOffset baseOffset = static_cast<DiskOffset>(index) * static_cast<DiskOffset>(blockSize());
    return device().write(baseOffset, count * blockSize(), data.pointer());
}

ByteBuffer DiskBackedFileSystem::readBlock(unsigned index) const
{
#ifdef DBFS_DEBUG
    kprintf("DiskBackedFileSystem::readBlock %u\n", index);
#endif
    {
        InterruptDisabler disabler;
        auto it = m_blockCache.find(index);
        if (it != m_blockCache.end()) {
            return (*it).value;
        }
    }

    auto buffer = ByteBuffer::createUninitialized(blockSize());
    DiskOffset baseOffset = static_cast<DiskOffset>(index) * static_cast<DiskOffset>(blockSize());
    auto* bufferPointer = buffer.pointer();
    device().read(baseOffset, blockSize(), bufferPointer);
    ASSERT(buffer.size() == blockSize());

    {
        InterruptDisabler disabler;
        if (m_blockCache.size() >= 32)
            m_blockCache.removeOneRandomly();
        m_blockCache.set(index, buffer);
    }
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
    InterruptDisabler disabler;
    m_blockCache.clear();
}
