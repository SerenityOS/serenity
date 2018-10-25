#pragma once

#include "CharacterDevice.h"

class RandomDevice final : public CharacterDevice {
public:
    RandomDevice();
    virtual ~RandomDevice() override;

    virtual Unix::ssize_t read(byte* buffer, Unix::size_t bufferSize) override;
    virtual Unix::ssize_t write(const byte* buffer, Unix::size_t bufferSize) override;
    virtual bool hasDataAvailableForRead() const override;
};

