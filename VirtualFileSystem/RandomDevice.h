#pragma once

#include "CharacterDevice.h"

class RandomDevice final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    RandomDevice();
    virtual ~RandomDevice() override;

    virtual ssize_t read(byte* buffer, size_t bufferSize) override;
    virtual ssize_t write(const byte* buffer, size_t bufferSize) override;
    virtual bool hasDataAvailableForRead() const override;
};

