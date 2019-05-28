#pragma once

#include <Kernel/Devices/Device.h>

class CharacterDevice : public Device {
public:
    virtual ~CharacterDevice() override;

protected:
    CharacterDevice(unsigned major, unsigned minor)
        : Device(major, minor)
    {
    }

private:
    virtual bool is_character_device() const final { return true; }
};
