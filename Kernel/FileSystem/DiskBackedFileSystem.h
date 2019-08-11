#pragma once

#include "FileSystem.h"
#include <AK/ByteBuffer.h>

class DiskBackedFS : public FS {
public:
    virtual ~DiskBackedFS() override;

    DiskDevice& device() { return *m_device; }
    const DiskDevice& device() const { return *m_device; }

    virtual void flush_writes() override;

protected:
    explicit DiskBackedFS(NonnullRefPtr<DiskDevice>&&);

    ByteBuffer read_block(unsigned index) const;
    ByteBuffer read_blocks(unsigned index, unsigned count) const;

    bool write_block(unsigned index, const ByteBuffer&);
    bool write_blocks(unsigned index, unsigned count, const ByteBuffer&);

private:
    NonnullRefPtr<DiskDevice> m_device;
    HashMap<unsigned, ByteBuffer> m_write_cache;
};
