#pragma once

#include <AK/Types.h>
#include "Limits.h"

class CharacterDevice {
public:
    virtual ~CharacterDevice();

    virtual ssize_t read(byte* buffer, size_t bufferSize) = 0;
    virtual ssize_t write(const byte* buffer, size_t bufferSize) = 0;

protected:
    CharacterDevice() { }
};
