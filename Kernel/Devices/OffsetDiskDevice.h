#pragma once

#include <AK/RetainPtr.h>
#include <Kernel/Devices/DiskDevice.h>

class OffsetDiskDevice final : public DiskDevice {
public:
    static Retained<OffsetDiskDevice> create(Retained<DiskDevice>&& device, unsigned offset);
    virtual ~OffsetDiskDevice();

    virtual unsigned block_size() const override;
    virtual bool read_block(unsigned index, byte* out) const override;
    virtual bool write_block(unsigned index, const byte*) override;
    virtual bool read_blocks(unsigned index, word count, byte*) override;
    virtual bool write_blocks(unsigned index, word count, const byte*) override;

private:
    virtual const char* class_name() const override;

    OffsetDiskDevice(Retained<DiskDevice>&&, unsigned);

    Retained<DiskDevice> m_device;
    unsigned m_offset;
};
