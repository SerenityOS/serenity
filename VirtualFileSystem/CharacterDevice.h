#pragma once

#include <AK/Types.h>
#include "Limits.h"

class CharacterDevice {
public:
    virtual ~CharacterDevice();

    virtual Unix::ssize_t read(byte* buffer, Unix::size_t bufferSize) = 0;
    virtual Unix::ssize_t write(const byte* buffer, Unix::size_t bufferSize) = 0;

protected:
    CharacterDevice() { }
};
