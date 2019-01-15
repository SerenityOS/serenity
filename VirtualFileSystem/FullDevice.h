#pragma once

#include "CharacterDevice.h"

class FullDevice final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    FullDevice();
    virtual ~FullDevice() override;

    virtual ssize_t read(byte* buffer, size_t bufferSize) override;
    virtual ssize_t write(const byte* buffer, size_t bufferSize) override;
    virtual bool has_data_available_for_reading(Process&) const override;
    virtual bool can_write(Process&) const override { return true; }
};

