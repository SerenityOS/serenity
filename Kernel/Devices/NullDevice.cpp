#include "NullDevice.h"
#include "Limits.h"
#include <AK/StdLibExtras.h>
#include <AK/kstdio.h>

static NullDevice* s_the;

NullDevice& NullDevice::the()
{
    ASSERT(s_the);
    return *s_the;
}

NullDevice::NullDevice()
    : CharacterDevice(1, 3)
{
    s_the = this;
}

NullDevice::~NullDevice()
{
}

bool NullDevice::can_read(Process&) const
{
    return true;
}

ssize_t NullDevice::read(Process&, byte*, ssize_t)
{
    return 0;
}

ssize_t NullDevice::write(Process&, const byte*, ssize_t buffer_size)
{
    return min(GoodBufferSize, buffer_size);
}

