#include "ZeroDevice.h"
#include "Limits.h"
#include <AK/StdLibExtras.h>
#include <AK/kstdio.h>

ZeroDevice::ZeroDevice()
    : CharacterDevice(1, 5)
{
}

ZeroDevice::~ZeroDevice()
{
}

bool ZeroDevice::can_read(Process&) const
{
    return true;
}

ssize_t ZeroDevice::read(Process&, byte* buffer, size_t bufferSize)
{
    size_t count = min(GoodBufferSize, bufferSize);
    memset(buffer, 0, count);
    return count;
}

ssize_t ZeroDevice::write(Process&, const byte*, size_t bufferSize)
{
    return min(GoodBufferSize, bufferSize);
}

