#pragma once

#include <Kernel/Devices/Device.h>

class BlockDevice : public Device {
public:
    virtual ~BlockDevice() override;

    virtual Region* mmap(Process&, LinearAddress preferred_laddr, size_t offset, size_t size) = 0;

protected:
    BlockDevice(unsigned major, unsigned minor) : Device(major, minor) { }

private:
    virtual bool is_block_device() const final { return true; }
};
