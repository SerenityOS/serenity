#include "NullDevice.h"
#include "Limits.h"
#include <AK/StdLib.h>
#include <cstring>
#include <cstdio>

NullDevice::NullDevice()
{
}

NullDevice::~NullDevice()
{
}

ssize_t NullDevice::read(byte*, size_t)
{
    printf("read from null\n");
    return 0;
}

ssize_t NullDevice::write(const byte*, size_t bufferSize)
{
    return min(GoodBufferSize, bufferSize);
}

