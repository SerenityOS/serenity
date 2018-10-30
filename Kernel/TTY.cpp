#include "TTY.h"

TTY::TTY(unsigned major, unsigned minor)
    : CharacterDevice(major, minor)
{
}

TTY::~TTY()
{
}

ssize_t TTY::read(byte* buffer, size_t size)
{
    return 0;
}

ssize_t TTY::write(const byte* buffer, size_t size)
{
    return 0;
}

bool TTY::hasDataAvailableForRead() const
{
    return false;
}
