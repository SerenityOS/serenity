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

    bool read_block(unsigned index, u8* buffer, FileDescription* = nullptr) const;
    bool read_blocks(unsigned index, unsigned count, u8* buffer, FileDescription* = nullptr) const;

    bool write_block(unsigned index, const u8*, FileDescription* = nullptr);
    bool write_blocks(unsigned index, unsigned count, const u8*, FileDescription* = nullptr);

private:
    DiskCache& cache() const;
    void flush_specific_block_if_needed(unsigned index);

    NonnullRefPtr<DiskDevice> m_device;
    mutable OwnPtr<DiskCache> m_cache;
};
