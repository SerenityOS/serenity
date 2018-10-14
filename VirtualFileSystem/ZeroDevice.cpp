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

Unix::ssize_t ZeroDevice::read(byte* buffer, Unix::size_t bufferSize)
{
    printf("read from zero device\n");
    Unix::size_t count = min(GoodBufferSize, bufferSize);
    memset(buffer, 0, count);
    return count;
}

Unix::ssize_t ZeroDevice::write(const byte*, Unix::size_t bufferSize)
{
    return min(GoodBufferSize, bufferSize);
}

