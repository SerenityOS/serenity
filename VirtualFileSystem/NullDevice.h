#pragma once

#include "CharacterDevice.h"

class NullDevice final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    NullDevice();
    virtual ~NullDevice() override;

private:
    // ^CharacterDevice
    virtual ssize_t read(Process&, byte* buffer, size_t bufferSize) override;
    virtual ssize_t write(Process&, const byte* buffer, size_t bufferSize) override;
    virtual bool can_write(Process&) const override { return true; }
    virtual bool can_read(Process&) const override;
    virtual const char* class_name() const override { return "CharacterDevice"; }
};

