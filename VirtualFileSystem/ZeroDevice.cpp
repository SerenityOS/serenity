#include "ZeroDevice.h"
#include "Limits.h"
#include <AK/StdLib.h>
#include <AK/kstdio.h>

ZeroDevice::ZeroDevice()
{
}

ZeroDevice::~ZeroDevice()
{
}

bool ZeroDevice::hasDataAvailableForRead() const
{
    return true;
}

Unix::ssize_t ZeroDevice::read(byte* buffer, Unix::size_t bufferSize)
{
    kprintf("ZeroDevice: read from zero\n");
    Unix::size_t count = min(GoodBufferSize, bufferSize);
    memset(buffer, 0, count);
    return count;
}

Unix::ssize_t ZeroDevice::write(const byte*, Unix::size_t bufferSize)
{
    return min(GoodBufferSize, bufferSize);
}

