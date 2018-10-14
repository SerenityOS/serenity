#pragma once

#include "CharacterDevice.h"

class FullDevice final : public CharacterDevice {
public:
    FullDevice();
    virtual ~FullDevice();

    Unix::ssize_t read(byte* buffer, Unix::size_t bufferSize) override;
    Unix::ssize_t write(const byte* buffer, Unix::size_t bufferSize) override;
};

