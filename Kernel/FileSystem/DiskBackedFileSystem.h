#pragma once

#include "FileSystem.h"
#include <AK/ByteBuffer.h>

class DiskCache;

class DiskBackedFS : public FS {
public:
    virtual ~DiskBackedFS() override;

    virtual bool is_disk_backed() const override { return true; }

    DiskDevice& device() { return *m_device; }
    const DiskDevice& device() const { return *m_device; }

    virtual void flush_writes() override;

    void flush_writes_impl();

protected:
    explicit DiskBackedFS(NonnullRefPtr<DiskDevice>&&);

    bool read_block(unsigned index, u8* buffer) const;
    bool read_blocks(unsigned index, unsigned count, u8* buffer) const;

    bool write_block(unsigned index, const u8*);
    bool write_blocks(unsigned index, unsigned count, const u8*);

private:
    DiskCache& cache() const;

    NonnullRefPtr<DiskDevice> m_device;
    mutable OwnPtr<DiskCache> m_cache;
};
