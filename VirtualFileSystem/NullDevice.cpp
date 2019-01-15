#include "NullDevice.h"
#include "Limits.h"
#include <AK/StdLibExtras.h>
#include <AK/kstdio.h>

NullDevice::NullDevice()
    : CharacterDevice(1, 3)
{
}

NullDevice::~NullDevice()
{
}

bool NullDevice::can_read(Process&) const
{
    return true;
}

ssize_t NullDevice::read(Process&, byte*, size_t)
{
    return 0;
}

ssize_t NullDevice::write(Process&, const byte*, size_t bufferSize)
{
    return min(GoodBufferSize, bufferSize);
}

