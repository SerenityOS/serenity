#include "DeviceBackedFileSystem.h"

//#define DBFS_DEBUG

DeviceBackedFileSystem::DeviceBackedFileSystem(RetainPtr<BlockDevice>&& device)
    : m_device(std::move(device))
{
    ASSERT(m_device);
}

DeviceBackedFileSystem::~DeviceBackedFileSystem()
{
}

bool DeviceBackedFileSystem::writeBlock(unsigned index, const ByteBuffer& data)
{
    ASSERT(data.size() == blockSize());
#ifdef DBFS_DEBUG
    printf("DeviceBackedFileSystem::writeBlock %u\n", index);
#endif
    qword baseOffset = static_cast<qword>(index) * static_cast<qword>(blockSize());
    return device().write(baseOffset, blockSize(), data.pointer());
}

bool DeviceBackedFileSystem::writeBlocks(unsigned index, unsigned count, const ByteBuffer& data)
{
#ifdef DBFS_DEBUG
    printf("DeviceBackedFileSystem::writeBlocks %u x%u\n", index, count);
#endif
    qword baseOffset = static_cast<qword>(index) * static_cast<qword>(blockSize());
    return device().write(baseOffset, count * blockSize(), data.pointer());
}

ByteBuffer DeviceBackedFileSystem::readBlock(unsigned index) const
{
#ifdef DBFS_DEBUG
    printf("DeviceBackedFileSystem::readBlock %u\n", index);
#endif
    auto buffer = ByteBuffer::createUninitialized(blockSize());
    qword baseOffset = static_cast<qword>(index) * static_cast<qword>(blockSize());
    auto* bufferPointer = buffer.pointer();
    device().read(baseOffset, blockSize(), bufferPointer);
    ASSERT(buffer.size() == blockSize());
    return buffer;
}

ByteBuffer DeviceBackedFileSystem::readBlocks(unsigned index, unsigned count) const
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

void DeviceBackedFileSystem::setBlockSize(unsigned blockSize)
{
    if (blockSize == m_blockSize)
        return;
    m_blockSize = blockSize;
    invalidateCaches();
}

void DeviceBackedFileSystem::invalidateCaches()
{
    // FIXME: Implement block cache.
}
