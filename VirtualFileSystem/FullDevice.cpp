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

Unix::ssize_t FullDevice::read(byte* buffer, Unix::size_t bufferSize)
{
    printf("read from full device\n");
    Unix::size_t count = min(GoodBufferSize, bufferSize);
    memset(buffer, 0, count);
    return count;
}

Unix::ssize_t FullDevice::write(const byte*, Unix::size_t bufferSize)
{
    if (bufferSize == 0)
        return 0;
    return -ENOSPC;
}

