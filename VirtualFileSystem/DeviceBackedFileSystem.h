#pragma once

#include "FileSystem.h"
#include <AK/ByteBuffer.h>

class DeviceBackedFileSystem : public FileSystem {
public:
    virtual ~DeviceBackedFileSystem() override;

    BlockDevice& device() { return *m_device; }
    const BlockDevice& device() const { return *m_device; }

    unsigned blockSize() const { return m_blockSize; }

protected:
    explicit DeviceBackedFileSystem(RetainPtr<BlockDevice>&&);

    void setBlockSize(unsigned);
    void invalidateCaches();

    ByteBuffer readBlock(unsigned index) const;
    ByteBuffer readBlocks(unsigned index, unsigned count) const;

    bool writeBlock(unsigned index, const ByteBuffer&);
    bool writeBlocks(unsigned index, unsigned count, const ByteBuffer&);

private:
    unsigned m_blockSize { 0 };
    RetainPtr<BlockDevice> m_device;
};
