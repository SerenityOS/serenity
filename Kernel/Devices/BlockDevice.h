#pragma once

#include <Kernel/Devices/Device.h>

class BlockDevice : public Device {
public:
    virtual ~BlockDevice() override;

    size_t block_size() const { return m_block_size; }
    virtual bool is_seekable() const override { return true; }

protected:
    BlockDevice(unsigned major, unsigned minor, size_t block_size = PAGE_SIZE)
        : Device(major, minor)
        , m_block_size(block_size)
    {
    }

private:
    virtual bool is_block_device() const final { return true; }

    size_t m_block_size { 0 };
};
