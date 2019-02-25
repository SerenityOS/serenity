#pragma once

#include "CharacterDevice.h"

class NullDevice final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    NullDevice();
    virtual ~NullDevice() override;

    static NullDevice& the();

private:
    // ^CharacterDevice
    virtual ssize_t read(Process&, byte*, ssize_t) override;
    virtual ssize_t write(Process&, const byte*, ssize_t) override;
    virtual bool can_write(Process&) const override { return true; }
    virtual bool can_read(Process&) const override;
    virtual const char* class_name() const override { return "NullDevice"; }
};

