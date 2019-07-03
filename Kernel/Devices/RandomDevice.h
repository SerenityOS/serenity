#pragma once

#include "CharacterDevice.h"

class RandomDevice final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    RandomDevice();
    virtual ~RandomDevice() override;

    static u32 random_value();

private:
    // ^CharacterDevice
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;
    virtual bool can_read(FileDescription&) const override;
    virtual bool can_write(FileDescription&) const override { return true; }
    virtual const char* class_name() const override { return "RandomDevice"; }
};
