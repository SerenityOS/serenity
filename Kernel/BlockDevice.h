#pragma once

#include <Kernel/Device.h>

class BlockDevice : public Device {
public:
    virtual ~BlockDevice() override;

protected:
    BlockDevice(unsigned major, unsigned minor) : Device(major, minor) { }
};
