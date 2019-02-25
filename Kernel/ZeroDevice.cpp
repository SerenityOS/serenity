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

ssize_t ZeroDevice::read(Process&, byte* buffer, ssize_t size)
{
    ssize_t count = min(GoodBufferSize, size);
    memset(buffer, 0, (size_t)count);
    return count;
}

ssize_t ZeroDevice::write(Process&, const byte*, ssize_t size)
{
    return min(GoodBufferSize, size);
}

