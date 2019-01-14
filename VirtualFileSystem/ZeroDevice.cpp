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

bool ZeroDevice::has_data_available_for_reading(Process&) const
{
    return true;
}

ssize_t ZeroDevice::read(byte* buffer, size_t bufferSize)
{
    size_t count = min(GoodBufferSize, bufferSize);
    memset(buffer, 0, count);
    return count;
}

ssize_t ZeroDevice::write(const byte*, size_t bufferSize)
{
    return min(GoodBufferSize, bufferSize);
}

