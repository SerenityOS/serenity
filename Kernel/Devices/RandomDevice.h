#pragma once

#include "CharacterDevice.h"

class RandomDevice final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    RandomDevice();
    virtual ~RandomDevice() override;

    static dword random_value();

private:
    // ^CharacterDevice
    virtual ssize_t read(FileDescriptor&, byte*, ssize_t) override;
    virtual ssize_t write(FileDescriptor&, const byte*, ssize_t) override;
    virtual bool can_read(FileDescriptor&) const override;
    virtual bool can_write(FileDescriptor&) const override { return true; }
    virtual const char* class_name() const override { return "RandomDevice"; }
};
