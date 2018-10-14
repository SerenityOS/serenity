#pragma once

#include "CharacterDevice.h"

class ZeroDevice final : public CharacterDevice {
public:
    ZeroDevice();
    virtual ~ZeroDevice();

    Unix::ssize_t read(byte* buffer, Unix::size_t bufferSize) override;
    Unix::ssize_t write(const byte* buffer, Unix::size_t bufferSize) override;
};

