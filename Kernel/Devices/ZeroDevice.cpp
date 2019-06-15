#include "ZeroDevice.h"
#include <AK/StdLibExtras.h>
#include <AK/kstdio.h>

ZeroDevice::ZeroDevice()
    : CharacterDevice(1, 5)
{
}

ZeroDevice::~ZeroDevice()
{
}

bool ZeroDevice::can_read(FileDescription&) const
{
    return true;
}

ssize_t ZeroDevice::read(FileDescription&, byte* buffer, ssize_t size)
{
    ssize_t count = min(PAGE_SIZE, size);
    memset(buffer, 0, (size_t)count);
    return count;
}

ssize_t ZeroDevice::write(FileDescription&, const byte*, ssize_t size)
{
    return min(PAGE_SIZE, size);
}
