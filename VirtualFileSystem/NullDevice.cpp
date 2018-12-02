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

ssize_t NullDevice::read(byte*, size_t)
{
    return 0;
}

ssize_t NullDevice::write(const byte*, size_t bufferSize)
{
    return min(GoodBufferSize, bufferSize);
}

