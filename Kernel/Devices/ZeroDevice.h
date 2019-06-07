#pragma once

#include "CharacterDevice.h"

class ZeroDevice final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    ZeroDevice();
    virtual ~ZeroDevice() override;

private:
    // ^CharacterDevice
    virtual ssize_t read(FileDescription&, byte*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const byte*, ssize_t) override;
    virtual bool can_read(FileDescription&) const override;
    virtual bool can_write(FileDescription&) const override { return true; }
    virtual const char* class_name() const override { return "ZeroDevice"; }
};
