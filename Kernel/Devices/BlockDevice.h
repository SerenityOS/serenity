#pragma once

#include <Kernel/Devices/Device.h>

class BlockDevice : public Device {
public:
    virtual ~BlockDevice() override;

    virtual bool is_seekable() const override { return true; }

protected:
    BlockDevice(unsigned major, unsigned minor)
        : Device(major, minor)
    {
    }

private:
    virtual bool is_block_device() const final { return true; }
};
