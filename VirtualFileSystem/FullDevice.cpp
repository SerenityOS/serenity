#include "FullDevice.h"
#include "Limits.h"
#include <LibC/errno_numbers.h>
#include <AK/StdLibExtras.h>
#include <AK/kstdio.h>

FullDevice::FullDevice()
    : CharacterDevice(1, 7)
{
}

FullDevice::~FullDevice()
{
}

bool FullDevice::has_data_available_for_reading() const
{
    return true;
}

ssize_t FullDevice::read(byte* buffer, size_t bufferSize)
{
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

