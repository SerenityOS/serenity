#pragma once

#include <AK/RefPtr.h>
#include <Kernel/Devices/DiskDevice.h>

class DiskPartition final : public DiskDevice {
public:
    static NonnullRefPtr<DiskPartition> create(NonnullRefPtr<DiskDevice>, unsigned block_offset);
    virtual ~DiskPartition();

    virtual unsigned block_size() const override;
    virtual bool read_block(unsigned index, u8* out) const override;
    virtual bool write_block(unsigned index, const u8*) override;
    virtual bool read_blocks(unsigned index, u16 count, u8*) override;
    virtual bool write_blocks(unsigned index, u16 count, const u8*) override;

    // ^BlockDevice
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override { return 0; }
    virtual bool can_read(FileDescription&) const override { return true; }
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override { return 0; }
    virtual bool can_write(FileDescription&) const override { return true; }

private:
    virtual const char* class_name() const override;

    DiskPartition(NonnullRefPtr<DiskDevice>, unsigned block_offset);

    NonnullRefPtr<DiskDevice> m_device;
    unsigned m_block_offset;
};
