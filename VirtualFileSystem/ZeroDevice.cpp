#include "ZeroDevice.h"
#include "Limits.h"
#include <AK/StdLib.h>
#include <cstring>
#include <cstdio>

ZeroDevice::ZeroDevice()
{
}

ZeroDevice::~ZeroDevice()
{
}

ssize_t ZeroDevice::read(byte* buffer, size_t bufferSize)
{
    printf("read from zero device\n");
    size_t count = min(GoodBufferSize, bufferSize);
    memset(buffer, 0, count);
    return count;
}

ssize_t ZeroDevice::write(const byte*, size_t bufferSize)
{
    return min(GoodBufferSize, bufferSize);
}

