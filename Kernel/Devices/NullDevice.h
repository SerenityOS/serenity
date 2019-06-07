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
    virtual ssize_t read(FileDescription&, byte*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const byte*, ssize_t) override;
    virtual bool can_write(FileDescription&) const override { return true; }
    virtual bool can_read(FileDescription&) const override;
    virtual const char* class_name() const override { return "NullDevice"; }
};
