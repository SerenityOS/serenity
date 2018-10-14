#include "FullDevice.h"
#include "Limits.h"
#include "sys-errno.h"
#include <AK/StdLib.h>
#include <cstring>
#include <cstdio>

FullDevice::FullDevice()
{
}

FullDevice::~FullDevice()
{
}

ssize_t FullDevice::read(byte* buffer, size_t bufferSize)
{
    printf("read from full device\n");
    size_t count = min(GoodBufferSize, bufferSize);
    memset(buffer, 0, count);
    return count;
}

ssize_t FullDevice::write(const byte*, size_t bufferSize)
{
    if (bufferSize == 0)
        return 0;
    return -ENOSPC;
}

