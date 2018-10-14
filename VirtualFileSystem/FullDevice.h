#pragma once

#include "CharacterDevice.h"

class FullDevice final : public CharacterDevice {
public:
    FullDevice();
    virtual ~FullDevice();

    ssize_t read(byte* buffer, size_t bufferSize) override;
    ssize_t write(const byte* buffer, size_t bufferSize) override;
};

