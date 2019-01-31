#pragma once

#include "FileSystem.h"
#include <AK/ByteBuffer.h>

class DiskBackedFS : public FS {
public:
    virtual ~DiskBackedFS() override;

    DiskDevice& device() { return *m_device; }
    const DiskDevice& device() const { return *m_device; }

    size_t block_size() const { return m_block_size; }

protected:
    explicit DiskBackedFS(RetainPtr<DiskDevice>&&);

    void set_block_size(unsigned);

    ByteBuffer read_block(unsigned index) const;
    ByteBuffer read_blocks(unsigned index, unsigned count) const;

    bool write_block(unsigned index, const ByteBuffer&);
    bool write_blocks(unsigned index, unsigned count, const ByteBuffer&);

private:
    size_t m_block_size { 0 };
    RetainPtr<DiskDevice> m_device;
};
