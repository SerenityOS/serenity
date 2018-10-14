#pragma once

#include "CharacterDevice.h"

class ZeroDevice final : public CharacterDevice {
public:
    ZeroDevice();
    virtual ~ZeroDevice();

    ssize_t read(byte* buffer, size_t bufferSize) override;
    ssize_t write(const byte* buffer, size_t bufferSize) override;
};

