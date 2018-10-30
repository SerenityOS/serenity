#include "NullDevice.h"
#include "Limits.h"
#include <AK/StdLib.h>
#include <AK/kstdio.h>

NullDevice::NullDevice()
    : CharacterDevice(1, 3)
{
}

NullDevice::~NullDevice()
{
}

bool NullDevice::hasDataAvailableForRead() const
{
    return true;
}

Unix::ssize_t NullDevice::read(byte*, Unix::size_t)
{
    return 0;
}

Unix::ssize_t NullDevice::write(const byte*, Unix::size_t bufferSize)
{
    return min(GoodBufferSize, bufferSize);
}

